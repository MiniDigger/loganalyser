package me.minidigger.loganalyser;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "networkName", "privateChannel"})})
public class Channel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private String networkName;
    private boolean privateChannel;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Line> lines;

    public Channel(String name, String networkName, boolean privateChannel, List<Line> lines) {
        this.name = name;
        this.networkName = networkName;
        this.privateChannel = privateChannel;
        this.lines = lines;
    }

    protected Channel() {
        // JPA
    }
}
