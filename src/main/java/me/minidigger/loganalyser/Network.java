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
public class Network {

    @Id
    private String name;
    @OneToMany
    private List<Channel> channels;

    protected Network() {
        // JPA
    }
}
