package colombo.searchengine.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "indexes")
public class IndexEntity {

    public IndexEntity() {}

    public IndexEntity(Float rank, LemmaEntity lemmaId, PageEntity pageId) {
        this.rank = rank;
        this.lemmaId = lemmaId;
        this.pageId = pageId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ranking", columnDefinition = "FLOAT NOT NULL")
    private Float rank;

    @ManyToOne
    @JoinColumn(name = "lemma_id")
    private LemmaEntity lemmaId;

    @ManyToOne
    @JoinColumn(name = "page_id")
    private PageEntity pageId;

}
