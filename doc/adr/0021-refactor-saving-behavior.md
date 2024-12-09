# 21. Refactoring the Saving Behavior to Use a Pinia Store and JSON Patches

Date: 2024-08-06

## Status

Accepted

## Context

Our current system employs an autosave feature that saves the entire documentation unit every 10 seconds or when the user manually clicks the save button. Sending the entire documentation unit, which can sometimes amount to megabytes of data, could be highly inefficient. Also, this method poses challenges for parallel work in documentation units and can be noisy, leading to potential conflicts when multiple users edit the same document concurrently.

Furthermore, we encountered reactivity problems by misusing prop drilling, passing the model value of the whole documentation unit down to its single input fields. Excessive prop drilling, especially when passing data through multiple nested components, that don't directly use it, is considered an antipattern in Vue. can lead to a more complex and harder-to-maintain codebase. Using state management solutions, such as Vuex or Pinia in Vue, can help avoid this antipattern by providing a centralized store for managing state.

To address these issues, we aim to implement partial saving using JSON patches. This approach involves saving only the changes made to a documentation unit, thereby enabling smoother collaboration, reducing the payload size and improving the overall user experience.

## Decision

### Frontend
1. Pinia Store Implementation: Create a Pinia store for managing the state of a documentation unit.
   Use loadDocumentUnit method from the store for initial loading in [documentNumber].vue.
   Use updateDocumentUnit method from the store across the software to update the docunit.
2. Install the fast-json-patch library. Adapt the update method in documentUnitService to send and receive patches. Generate a JSON Patch on update by comparing the updated documentation unit with a previous copy (originalDocumentUnit).

### Backend
1. Use the json-patch-path library to map JSON Patch syntax to our Java model objects.
2. Adapt update method to handle patches and versions of documentation units, stored in the table `documentation_unit_patch`


## Consequences

Refactoring the saving behavior to use a Pinia store and JSON patches will enhance the system's ability to handle parallel work efficiently, provide a smoother user experience and lay the groundwork for detailed versioning and undo/redo functionality. This approach aligns with modern software development practices and addresses the challenges of collaborative document editing.

### Positive:

- Improved user experience with reduced conflicts.
- Enhanced performance by reducing payload size.
- Better handling of parallel work and versioning.

### Negative:

- Initial complexity in refactoring the existing codebase.
- Potential challenges in managing version conflicts.

By adopting this approach, we aim to modernize our saving mechanism, making it more efficient and user-friendly.
