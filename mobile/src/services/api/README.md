# services/api

API integration layer (HLD §19), isolated from screens.

Planned:

- `client.ts` — base URL, headers, Bearer auth header, response parsing of the
  `ApiResponse` envelope, and common error handling.
- Feature APIs: `authApi.ts`, `loanApi.ts`, `creditCardApi.ts`, `expenseApi.ts`,
  `paymentApi.ts`.

Screens never make raw HTTP calls; they go through TanStack Query hooks that call
these APIs. Fleshed out during UI TASK 4 (auth) and the API-integration phase.

Not implemented yet in UI TASK 1 (foundation).
