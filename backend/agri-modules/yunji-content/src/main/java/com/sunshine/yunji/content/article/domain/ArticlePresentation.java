package com.sunshine.yunji.content.article.domain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
/** Optional versioned fields; plain text is rendered as text, never executable markup. */
public record ArticlePresentation(@Size(max=300) String fit,@Size(max=100) String duration,@Size(max=100) String level,
 @Size(max=30) List<@NotNull @Valid Step> steps,@Size(max=20) List<@NotBlank @Size(max=1000) String> tips,
 @Size(max=10) List<@NotNull @Valid Contact> contacts,@Size(max=10) List<@NotNull @Positive Long> relatedArticleIds) {
 public record Step(@NotBlank @Size(max=150) String title,@NotBlank @Size(max=3000) String description){}
 public record Contact(@NotBlank @Size(max=100) String label,@NotBlank @Size(max=500) String value){}
 public static ArticlePresentation empty(){return new ArticlePresentation(null,null,null,List.of(),List.of(),List.of(),List.of());}
}
