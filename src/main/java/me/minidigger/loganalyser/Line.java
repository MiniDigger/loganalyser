package me.minidigger.loganalyser;

import java.io.Serializable;
import java.time.LocalTime;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"time", "user", "content", "extra", "type"})})
public class Line implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalTime time;
    private String user;
    @Lob
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
}
