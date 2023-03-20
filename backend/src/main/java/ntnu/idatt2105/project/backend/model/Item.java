package ntnu.idatt2105.project.backend.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "items")
@Schema(description = "The item that is listed in the marketplace")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "The id of the item, automatically generated")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @Schema(description = "The User who posted the Item")
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    @Schema(description = "The category of the item")
    private Category category;

    @Column(name = "title")
    @Schema(description = "The title of the item")
    private String title;

    @Column(name = "brief_description")
    @Schema(description = "Brief description of the item")
    private String briefDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    @Schema(description = "Full description of the item")
    private String description;

    @Column(name = "price")
    @Schema(description = "The price of the item")
    private BigDecimal price;

    @Column(name = "longitude")
    @Schema(description = "The longitude position of the item")
    private String longitude;

    @Column(name = "latitude")
    @Schema(description = "The latitude position of the item")
    private String latitude;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    @Schema(description = "A string representation of all image urls for the item")
    private String imageUrls;
}
