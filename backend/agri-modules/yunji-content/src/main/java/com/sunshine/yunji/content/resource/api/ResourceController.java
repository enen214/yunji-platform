package com.sunshine.yunji.content.resource.api;
import com.sunshine.yunji.content.shared.contract.*;
import com.sunshine.yunji.content.resource.application.ResourceService;
import com.sunshine.yunji.content.shared.*;
import org.springframework.web.bind.annotation.*;
// Retired by the 2026-10-27 baseline: historical Java types remain for audit/FK readers.
// This class is deliberately not a Spring controller.
public class ResourceController {
 private final ResourceService s;public ResourceController(ResourceService s){this.s=s;}
 @GetMapping("/api/v1/resources/subjects") public ApiResponse<java.util.List<Views.PublicSubjectView>> subjects(){return ApiResponse.ok(ViewMapper.list(s.subjects(),Views.PublicSubjectView.class));}
 @GetMapping("/api/v1/resources/dimensions") public ApiResponse<java.util.List<Views.PublicDimensionView>> dimensions(){return ApiResponse.ok(ViewMapper.list(s.dimensions(),Views.PublicDimensionView.class));}
 @GetMapping("/api/v1/resources/packages") public ApiResponse<java.util.List<Views.PublicResourceView>> packages(){return ApiResponse.ok(ViewMapper.list(s.packages(false),Views.PublicResourceView.class));}
 @GetMapping("/api/v1/resources/packages/{id}") public ApiResponse<Views.PublicResourceView> resource(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.resource(id,false),Views.PublicResourceView.class));}
 @GetMapping("/api/v1/resources/exams") public ApiResponse<java.util.List<Views.PublicExamView>> exams(){return ApiResponse.ok(ViewMapper.list(s.exams(),Views.PublicExamView.class));}
 @GetMapping("/api/v1/resources/artifacts") public ApiResponse<java.util.List<Views.PublicArtifactView>> artifacts(){return ApiResponse.ok(ViewMapper.list(s.artifacts(false),Views.PublicArtifactView.class));}
 @GetMapping("/api/v1/resources/artifacts/{id}") public ApiResponse<Views.PublicArtifactView> artifact(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.artifact(id,false),Views.PublicArtifactView.class));}
 @PostMapping("/api/v1/admin/resources/subjects") public ApiResponse<Views.SubjectView> subject(@RequestBody ResourceService.Subject b){return ApiResponse.ok(ViewMapper.of(s.subject(b),Views.SubjectView.class));}
 @GetMapping("/api/v1/admin/resources/subjects") public ApiResponse<java.util.List<Views.SubjectView>> adminSubjects(){return ApiResponse.ok(ViewMapper.list(s.adminSubjects(),Views.SubjectView.class));}
 @PutMapping("/api/v1/admin/resources/subjects/{id}") public ApiResponse<Views.SubjectView> editSubject(@PathVariable long id,@RequestBody ResourceService.SubjectUpdate b){return ApiResponse.ok(ViewMapper.of(s.subject(id,b),Views.SubjectView.class));}
 @PostMapping("/api/v1/admin/resources/dimensions") public ApiResponse<Views.DimensionView> dimension(@RequestBody ResourceService.Dimension b){return ApiResponse.ok(ViewMapper.of(s.dimension(b),Views.DimensionView.class));}
 @GetMapping("/api/v1/admin/resources/dimensions") public ApiResponse<java.util.List<Views.DimensionView>> adminDimensions(){return ApiResponse.ok(ViewMapper.list(s.adminDimensions(),Views.DimensionView.class));}
 @PutMapping("/api/v1/admin/resources/dimensions/{id}") public ApiResponse<Views.DimensionView> editDimension(@PathVariable long id,@RequestBody ResourceService.DimensionUpdate b){return ApiResponse.ok(ViewMapper.of(s.dimension(id,b),Views.DimensionView.class));}
 @GetMapping("/api/v1/admin/resources/packages") public ApiResponse<java.util.List<Views.ResourceView>> adminPackages(){return ApiResponse.ok(ViewMapper.list(s.packages(true),Views.ResourceView.class));}
 @GetMapping("/api/v1/admin/resources/packages/{id}") public ApiResponse<Views.ResourceView> adminResource(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.resource(id,true),Views.ResourceView.class));}
 @PostMapping("/api/v1/admin/resources/packages") public ApiResponse<Views.ResourceView> create(@RequestBody ResourceService.Resource b){return ApiResponse.ok(ViewMapper.of(s.resource(null,b),Views.ResourceView.class));}
 @PutMapping("/api/v1/admin/resources/packages/{id}") public ApiResponse<Views.ResourceView> edit(@PathVariable long id,@RequestBody ResourceService.Resource b){return ApiResponse.ok(ViewMapper.of(s.resource(id,b),Views.ResourceView.class));}
 @PostMapping("/api/v1/admin/resources/exams") public ApiResponse<Views.ExamView> exam(@RequestBody ResourceService.Exam b){return ApiResponse.ok(ViewMapper.of(s.exam(b),Views.ExamView.class));}
 @GetMapping("/api/v1/admin/resources/exams") public ApiResponse<java.util.List<Views.ExamView>> adminExams(){return ApiResponse.ok(ViewMapper.list(s.exams(true),Views.ExamView.class));}
 @PutMapping("/api/v1/admin/resources/exams/{id}") public ApiResponse<Views.ExamView> editExam(@PathVariable long id,@RequestBody ResourceService.ExamUpdate b){return ApiResponse.ok(ViewMapper.of(s.exam(id,b),Views.ExamView.class));}
 @GetMapping("/api/v1/admin/resources/artifacts") public ApiResponse<java.util.List<Views.ArtifactView>> adminArtifacts(){return ApiResponse.ok(ViewMapper.list(s.artifacts(true),Views.ArtifactView.class));}
 @GetMapping("/api/v1/admin/resources/artifacts/{id}") public ApiResponse<Views.ArtifactView> adminArtifact(@PathVariable long id){return ApiResponse.ok(ViewMapper.of(s.artifact(id,true),Views.ArtifactView.class));}
 @PostMapping("/api/v1/admin/resources/artifacts") public ApiResponse<Views.ArtifactView> createArtifact(@RequestBody ResourceService.Artifact b){return ApiResponse.ok(ViewMapper.of(s.artifact(b),Views.ArtifactView.class));}
 @PutMapping("/api/v1/admin/resources/artifacts/{id}") public ApiResponse<Views.ArtifactView> editArtifact(@PathVariable long id,@RequestBody ResourceService.ArtifactUpdate b){return ApiResponse.ok(ViewMapper.of(s.artifact(id,b),Views.ArtifactView.class));}
 @PostMapping("/api/v1/admin/resources/artifacts/{id}/versions") public ApiResponse<Views.ArtifactView> release(@PathVariable long id,@RequestBody ResourceService.Release b){return ApiResponse.ok(ViewMapper.of(s.release(id,b),Views.ArtifactView.class));}
 @DeleteMapping("/api/v1/admin/resources/{kind:subjects|dimensions|packages|exams|artifacts}/{id}") public ApiResponse<Void> withdraw(@PathVariable String kind,@PathVariable long id,@RequestParam int expectedVersion){s.withdraw(kind,id,expectedVersion);return ApiResponse.ok(null);}
}
