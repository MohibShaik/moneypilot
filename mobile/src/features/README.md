# features

Feature modules (HLD §5). Each feature owns its screens, feature-specific
components, hooks, and isolated mock data (`<feature>/mock/`). Shared UI lives in
`src/components`; shared server-state hooks wrap TanStack Query.

Features: auth, dashboard, loans, creditCards, expenses, payments, profile.

Built incrementally per the UI task sequence (HLD §28):
auth → dashboard → loans → creditCards → expenses → payments → profile.
