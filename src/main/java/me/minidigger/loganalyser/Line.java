package me.minidigger.loganalyser;

import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.time.LocalTime;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class Line implements Serializable {

    @Id
    @GenericGenerator(name = "HashCodeGenerator", strategy = "me.minidigger.loganalyser.HashCodeGenerator")
    @GeneratedValue(generator = "HashCodeGenerator")
    private int id;

    private LocalTime time;
    private String user;
    private String content;
    private String extra;
    @Enumerated(EnumType.STRING)
    private LineType type;

    public Line(LocalTime time, String user, String content, String extra, LineType type) {
        this.time = time;
        this.user = user;
        this.content = content;
        this.extra = extra;
        this.type = type;
    }

    protected Line() {
        // JPA
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (extra != null ? extra.hashCode() : 0);
        result = 31 * result + (type != null ? type.ordinal() : 0);
        return result;
    }
}
