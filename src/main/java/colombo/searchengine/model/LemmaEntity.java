package colombo.searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "lemma", uniqueConstraints = { @UniqueConstraint(columnNames = {"lemma", "site_id" }) })
public class LemmaEntity {

    public LemmaEntity() {}

    public LemmaEntity(String lemma, Integer frequency, SiteEntity site) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.site = site;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;


    @Column(name = "lemma", columnDefinition = "VARCHAR(255) NOT NULL")
    private String lemma;

    @Column(name = "frequency", columnDefinition = "INT NOT NULL DEFAULT 1")
    private Integer frequency;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteEntity site;

    @OneToMany(mappedBy = "lemmaId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexEntity> indexes;

}
