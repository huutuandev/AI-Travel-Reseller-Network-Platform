This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
# Local role demo accounts

Use these development-only accounts on `/login` to preview the role dashboards. The credentials are mock data stored in the frontend and do not authenticate against a server.

| Role | Email | Password |
| --- | --- | --- |
| Platform Admin | `admin@wayfarer.test` | `AdminDemo2026!` |
| Tenant | `avery@lantern-tide.test` | `TenantDemo2026!` |
| Reseller | `jordan@partner.test` | `ResellerDemo2026!` |

The role account definitions live in `lib/mock-auth.ts`. Replace the frontend mock adapter when a real authentication service is available. This implementation is for local demonstration only; backend authorization is required for production.

## Customer storefront demo account

Use the shared `/login` page to preview the customer sign-in flow. Storefront links open it with the storefront return path; customer accounts use a dedicated browser session and do not share internal dashboard roles. `/storefront/login` remains as a compatibility redirect.

| Role | Email | Password |
| --- | --- | --- |
| Storefront customer | `traveler@lantern-tide.test` | `TravelDemo2026!` |

Customer mock account logic is in `lib/customer-auth.ts`. New customer sign-ups are stored in the current browser only. Sample checkout requests are also browser-local; no payment or reservation is sent to a server.

## Storefront photo credits

The demonstration destination photos are local frontend assets sourced from Unsplash. Each source page marks its photo free to use under the Unsplash License:

- Ha Long Bay: [Pierre Bonvalot](https://unsplash.com/photos/limestone-karsts-rise-from-the-water-in-a-scenic-bay-wPquuVxGqBc)
- Hoi An: [Rebecca N.](https://unsplash.com/photos/a-bunch-of-lanterns-are-lit-up-in-the-dark-oBVH5LSpWXE)
- Phu Quoc: [Vivu Vietnam](https://unsplash.com/photos/a-beach-with-palm-trees-and-a-boat-in-the-water-IVJVh1v1PVs)
