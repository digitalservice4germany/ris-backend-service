# 16. Identify Reactive WebFlux as Technical Debt in Caselaw

Date: 2023-07-20

## Status

Accepted

Supersedes [3. Use Spring WebFlux reactive stack](0003-\[Superseded\]use-spring-webflux-reactive-stack.md)

## Context

While using Reactive WebFlux for over a year, the development team faced multiple challenges, leading to the reconsideration of its use.

### Challenge 1: Cognitive Load

Developing with Reactive WebFlux presented several issues, including:

- Complex, hard-to-read code
- Difficulty in debugging
- Extensive onboarding time for new engineers, as WebFlux is not widely used
- Development often devolving into inefficient trial-and-error mode

These issues led to a perceived decrease in productivity and code quality. The cognitive complexity was compounded by the team's composition of full-stack developers who are less familiar with the intricacies of WebFlux. Although some learning effects were observed, they did not reach the necessary extent. 

It was noted that both internal and external teams faced similar challenges (see [UseID](https://github.com/digitalservicebund/useid-backend-service/blob/main/doc/adr/0016-use-spring-mvc-instead-of-webflux.md)).

### Challenge 2: WebFlux Implementation Issues

WebFlux is integrated into Spring, which is inherently based on different imperative concepts. This necessitated overcoming the gap through additional boilerplate and workarounds.

WebFlux's use of R2DBC Database adapters for reactive database access, instead of JPA, was problematic for some use cases. As a result, JPA had to be used, leading to inconsistencies in the code.

The problems were further compounded by:

- The lack of in-depth resources on more complex topics due to WebFlux's lower popularity
- The incompatibility of many commonly used Spring ecosystem libraries with WebFlux, necessitating the use of less mature alternatives

Lastly, the frontend did not use the benefits from the reactive backend.

### Challenge 3: Absence of Reactive Backend Requirements

The advantages of reactive backends, such as fast response times, resilience, and scalability, do not align with the current needs of the NeuRIS Caselaw backend, which is designed for a relatively small user base (< 200) and does not emphasize response time optimization. Resilience is not a significant concern as the backend functions as a single service.

### Consideration & Decision Process

The project is still in the early stages, and the MVP is not yet released, making architectural improvements potentially worthwhile. However, the project's focus is on delivering features and releasing the MVP soon, which makes a full backend refactoring risky. The team carefully discussed this topic and conducted multiple workshops to ensure confidence in the decision.   

#### Alternatives

Spring WebMVC with JPA was considered as an alternative to Reactive WebFlux with R2DBC, given that the project already uses Spring and JPA. Other alternatives were not considered.

#### Risk & Cost

Considering the challenges, the team agreed that the original decision to use Reactive WebFlux might have been different with the current knowledge. However, a complete refactoring is associated with significant work and costs, which is of concern given the imminent release of the MVP and the team's unfamiliarity with JPA.

#### Refactoring Workshop

The team conducted a workshop to evaluate the potential costs and benefits of a refactoring. The exercise involved refactoring WebFlux Code to Spring WebMVC and JPA for two use cases.

The workshop resulted in the following observations:

**Benefits**

- Code becomes more readable
- Code base becomes leaner
- Opportunity to gain code ownership, discover existing bugs, and add documentation
- Improved debugging
- Manageable learning curve
- Some Entities already have Implementations for both R2DBC and JPA, commiting to one solution reduces the lines of code and  makes the code more consistent  

**Cost**

- Understanding existing code and business logic is time-consuming
- Refactoring is a lengthy process
- JPA concepts (e.g., Annotations) need to be learned by the team

**Other observations**

- The extent of parallelism in refactoring is uncertain
- The productivity increase for new features is unclear
- The impact on performance is unknown
- Many tests need to be adapted, but to an extent that still offers confidence in the correctness of the new code, especially since we can reuse the end 2 end tests completely
- No advantages of WebFlux have been noticed



## Decision

WebFlux is recognized as a technical debt that should eventually be replaced by Spring MVC and JPA.

This decision impacts the entire caselaw project. It is expected to improve code quality, reduce onboarding time for new engineers, and ultimately lead to a more stable and maintainable backend. This will directly contribute to the project's long-term sustainability and ease of feature addition.

## Consequences

**Next Steps**

The decision is aimed more at acknowledging technical debt than prompting immediate action. The team agreed to address the issue as follows:

- New features will be implemented using Spring WebMVC and JPA
- Existing code will be refactored to Spring WebMVC if the effort is deemed manageable
- A full-scale refactoring will be pursued as soon as possible, given the project timeline

**Potential Challenges**

- The refactoring's impact on the backend performance/response times is uncertain. A performance test should be conducted before complete refactoring.
- While Spring WebMVC and JPA offer more resources and mature libraries, challenges related to cognitive complexity and the learning curve may still arise. Training could focus on these areas to mitigate potential issues.
- Given the sizable existing code base (11,400 lines of Java source code and 14,000 lines of Java test code), the effort required for a complete refactoring may be underestimated.
- The Norms team has also encountered limits of R2DBC but decided to keep it due to other priorities. This decision could either support the plans of the Norms team or could result in a more significant gap between the two backend parts.
