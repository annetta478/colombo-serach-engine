package colombo.searchengine.model;

import lombok.Data;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "page", indexes =@Index(columnList = "path"))
public class PageEntity {

    public PageEntity() {}

    public PageEntity(Integer id, String path, Integer code, String content) {
        this.id = id;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public PageEntity(String path, Integer code, String content, SiteEntity site) {
        this.path = path;
        this.code = code;
        this.site = site;
        this.content = content;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "path", columnDefinition = "VARCHAR(255) NOT NULL")
    private String path;

    @Column(name = "code", nullable = false)
    private Integer code;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT NOT NULL")
    private String content;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private SiteEntity site;

    @OneToMany(mappedBy = "pageId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IndexEntity> indexes;

}
