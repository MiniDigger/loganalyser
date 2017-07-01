package me.minidigger.loganalyser;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Entity
@AllArgsConstructor
public class Channel {

    @Id
    private String name;
    private boolean privateChannel;
    @OneToMany
    private List<Line> lines;

    protected Channel() {
        // JPA
    }
}
