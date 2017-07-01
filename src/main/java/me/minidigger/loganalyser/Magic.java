package me.minidigger.loganalyser;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Magic {

    private static SessionFactory sessionFactory;

    private static final String user = "logs";
    private static final String pass = "superSecurePassword";
    private static final String driver = "com.mysql.jdbc.Driver";
    private static final String url = "jdbc:mysql://atlas.minidigger.me:3336/logs?useSSL=false";
    private static final String dialect = "org.hibernate.dialect.MySQL5Dialect";

    private static final Pattern messagePattern = Pattern.compile("(?<time>\\[.*?\\]) (?<sender><.+?>) (?<message>.*)");
    private static final Pattern actionPattern = Pattern.compile("(?<time>\\[.*?\\]) \\* (?<sender>.*) (?<message>.*)");
    private static final Pattern nickPattern = Pattern.compile("(?<time>\\[.*?\\]) \\*\\*\\* (?<oldname>.*) is now known as (?<newname>.*)");
    private static final Pattern joinsPattern = Pattern.compile("(?<time>\\[.*?\\]) \\*\\*\\* Joins: (?<name>.*) \\((?<hostmask>.*)\\)");
    private static final Pattern quitsPattern = Pattern.compile("(?<time>\\[.*?\\]) \\*\\*\\* Quits: (?<name>.*) \\((?<hostmask>.*)\\)");
    private static final Pattern partsPattern = Pattern.compile("(?<time>\\[.*?\\]) \\*\\*\\* Parts: (?<name>.*) \\((?<hostmask>.*)\\)");
    private static final Pattern modePattern = Pattern.compile("(?<time>\\[.*?\\]) \\*\\*\\* (?<name>.*) sets mode: (?<mode>.*) (?<dude>.*)");
    private static final Pattern kickPattern = Pattern.compile("(?<time>\\[.*?\\]) \\*\\*\\* (?<name>.*) was kicked by (?<op>.*) \\((?<reason>.*)\\)");

    public static void main(String[] args) throws InterruptedException {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                // credetials and stuff
                .applySetting("hibernate.connection.username", user)
                .applySetting("hibernate.connection.password", pass)
                .applySetting("hibernate.connection.driver_class", driver)
                .applySetting("hibernate.connection.url", url)
                .applySetting("hibernate.dialect", dialect)
                // misc settings
                .applySetting("hibernate.hbm2ddl.auto", "update")
                .applySetting("hibernate.show_sql", true + "")
                //TODO apparently this is an anti-pattern [0], but it fixes an issue so ¯\_(ツ)_/¯
                // [0]: https://vladmihalcea.com/2016/09/05/the-hibernate-enable_lazy_load_no_trans-anti-pattern/
                .applySetting("hibernate.enable_lazy_load_no_trans", true)
                .applySetting("hibernate.connection.autocommit", true)
                // connection pool
                .applySetting("hibernate.connection.pool_size", 10 + "")
                .build();

        MetadataSources sources = new MetadataSources(registry);

        sources.addAnnotatedClass(Network.class);
        sources.addAnnotatedClass(Channel.class);
        sources.addAnnotatedClass(Line.class);

        try {
            Metadata metadata = sources.buildMetadata();
            sessionFactory = metadata.buildSessionFactory();
        } catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
            e.printStackTrace();
        }

        //insertTestData();

        loadData();

        sessionFactory.close();
    }

    private static void loadData() {
        File root = new File("C:\\Users\\Martin\\Downloads\\temp\\log");
        File[] networks = root.listFiles();
        if (networks == null) {
            return;
        }

        Set<Network> networkList = Collections.newSetFromMap(new ConcurrentHashMap<>());
        // for each network
        Arrays.stream(networks).parallel().forEach(networkFile -> {
            System.out.println("NETWORK " + networkFile.getName());
            File[] channels = networkFile.listFiles();
            if (channels == null) {
                return;
            }

            Set<Channel> channelList = Collections.newSetFromMap(new ConcurrentHashMap<>());
            // for each channel
            Arrays.stream(channels).parallel().forEach(channelFile -> {
                System.out.println("CHANNEL " + channelFile.getName());
                File[] logs = channelFile.listFiles();
                if (logs == null) {
                    return;
                }

                // is Private chan?
                String name = channelFile.getName();
                boolean privateChan = true;
                if (name.startsWith("#")) {
                    privateChan = false;
                    name = name.replaceFirst("#", "");
                }
                final boolean fPrivateChan = privateChan;

                Set<Line> lines = Collections.newSetFromMap(new ConcurrentHashMap<>());
                // for each logfile
                Arrays.stream(logs).parallel().forEach(logFile -> {
                    try (Stream<String> readLines = Files.lines(logFile.toPath())) {
                        // for each line
                        readLines.parallel().forEach(line -> {
                            if (!line.startsWith("[")) {
                                System.out.println("UNPARSEABLE LINE IN FILE " + logFile.getAbsolutePath() + ": " + line);
                                return;
                            }

                            Date date;
                            String user;
                            String content;
                            String extra;
                            LineType type;
                            // system
                            if (line.contains("] *** ")) {
                                if (line.contains(" is now known as ")) {
                                    Matcher matcher = nickPattern.matcher(line);
                                    String dateString = matcher.group("time");
                                    date = parseDate(dateString);
                                    user = matcher.group("oldname");
                                    content = matcher.group("newname");
                                    type = LineType.NICK_CHANGE;
                                    extra = "";
                                } else if (line.contains("] *** Joins: ")) {
                                    Matcher matcher = joinsPattern.matcher(line);
                                    String dateString = matcher.group("time");
                                    date = parseDate(dateString);
                                    user = matcher.group("name");
                                    content = matcher.group("hostmask");
                                    type = LineType.JOIN;
                                    extra = "";
                                } else if (line.contains("] *** Quits: ")) {
                                    Matcher matcher = quitsPattern.matcher(line);
                                    String dateString = matcher.group("time");
                                    date = parseDate(dateString);
                                    user = matcher.group("name");
                                    content = matcher.group("hostmask");
                                    type = LineType.LEAVE;
                                    extra = "";
                                } else if (line.contains("] *** Parts: ")) {
                                    Matcher matcher = partsPattern.matcher(line);
                                    String dateString = matcher.group("time");
                                    date = parseDate(dateString);
                                    user = matcher.group("name");
                                    content = matcher.group("hostmask");
                                    extra = "";
                                    type = LineType.PART;
                                } else if (line.contains("] was kicked by ")) {
                                    Matcher matcher = partsPattern.matcher(line);
                                    String dateString = matcher.group("time");
                                    date = parseDate(dateString);
                                    user = matcher.group("name");
                                    content = matcher.group("reason");
                                    extra = matcher.group("op");
                                    type = LineType.KICK;
                                } else if (line.contains(" sets mode: ")) {
                                    Matcher matcher = modePattern.matcher(line);
                                    String dateString = matcher.group("time");
                                    date = parseDate(dateString);
                                    user = matcher.group("name");
                                    content = matcher.group("mode");
                                    extra = matcher.group("dude");
                                    type = LineType.MODE;
                                } else {
                                    System.out.println("COULD NOT PARSE LINE IN FILE " + logFile.getAbsolutePath() + ": " + line);
                                    return;
                                }
                            }
                            // action
                            else if (line.contains("] * ")) {
                                Matcher matcher = actionPattern.matcher(line);
                                String dateString = matcher.group("time");
                                date = parseDate(dateString);
                                user = matcher.group("sender");
                                content = matcher.group("content");
                                extra = "";
                                type = LineType.ACTION;
                            }
                            // message
                            else {
                                Matcher matcher = messagePattern.matcher(line);
                                if (!matcher.matches()) {
                                    System.out.println("MATCHER DID NOT MATCH " + line);
                                }
                                String dateString = matcher.group("time");
                                date = parseDate(dateString);
                                user = matcher.group("sender");
                                content = matcher.group("message");
                                extra = "";
                                type = fPrivateChan ? LineType.PRIVATE_MESSAGE : LineType.MESSAGE;
                            }

                            // create line and add to list
                            Line l = new Line(date, user, content, extra, type);
                            lines.add(l);
                        });
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        System.out.println("ERROR WHILE READING FILE " + logFile.getAbsolutePath());
                    }
                });

                // create channel and add to list
                List<Line> temp = new ArrayList<>(lines);
                Channel channel = new Channel(name, privateChan, temp);
                channelList.add(channel);

                System.out.println("FINISHED CHANNEL " + channelFile.getName());
            });

            // create network and add to list
            List<Channel> temp = new ArrayList<>(channelList);
            Network network = new Network(networkFile.getName(), temp);
            networkList.add(network);

            System.out.println("FINISH NETWORK " + networkFile.getName());
        });

        System.out.println("FINISHED! SAVING");
        session(session -> {
            networkList.forEach(network -> {
                network.getChannels().forEach(channel -> {
                    channel.getLines().forEach(session::saveOrUpdate);
                    session.saveOrUpdate(channel);
                });
                session.saveOrUpdate(network);
            });
        });
    }

    private static Date parseDate(String dateString) {
        try {
            return SimpleDateFormat.getTimeInstance().parse(dateString.replace("[", "").replace("]", ""));
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date(0);
        }
    }

    private static void insertTestData() {
        Network network = new Network("TestNetwork", new ArrayList<>());
        Channel channel = new Channel("TestChannel", false, new ArrayList<>());
        network.getChannels().add(channel);
        Line line = new Line(new Date(1337), "MiniDigger", "This is a test", "", LineType.MESSAGE);
        channel.getLines().add(line);

        session(session -> {
            session.saveOrUpdate(line);
            session.saveOrUpdate(channel);
            session.saveOrUpdate(network);
        });
    }

    private static <T> T session(SessionExecutor<T> executor) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();

            T t = executor.execute(session);

            session.getTransaction().commit();
            session.close();

            return t;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private static void session(SessionExecutor2 executor) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        executor.execute(session);

        session.getTransaction().commit();
        session.close();
    }

    @FunctionalInterface
    interface SessionExecutor<T> {
        T execute(Session session);
    }

    @FunctionalInterface
    interface SessionExecutor2 {
        void execute(Session session);
    }
}
