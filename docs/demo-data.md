# Local demo data

The opt-in `DemoDataSeedIT` seeded the configured **local** MySQL database `peerskillexchange`. It rejects remote JDBC targets, inserts records in one transaction, and can be rerun without duplicating data. It never changes the four accounts that were present before this seed. The seeder lives under `src/test`, so it is not packaged with the application.

Six verified `USER` accounts were created. Each can log in with **`Pass@123`**; passwords are stored as BCrypt hashes.

| Username | Teaches | Learns |
| --- | --- | --- |
| `demo_aarav` | React, TypeScript | Spring Boot, Java, Docker |
| `demo_meera` | Spring Boot, Java | React, CSS, Python |
| `demo_isha` | Figma, UI design | Docker, AWS, Python |
| `demo_kabir` | Docker, AWS | Figma, UI design, React |
| `demo_riya` | Python, machine learning | PostgreSQL, SQL, Spring Boot, Figma |
| `demo_dev` | PostgreSQL, SQL | Python, machine learning |

Emails use `<username>@example.test`; profiles use fictional names and a fictional college. The seed also created six exchange requests (three accepted, two pending, one rejected), three conversations, six chat messages, and three open empty whiteboard rooms. Eleven new skills were added; existing React and Spring Boot skill rows were reused.

To rerun, load the project's local `.env` values into the process environment and execute `mvnw.cmd -q -Dtest=DemoDataSeedIT test`. The test checks the JDBC target, existing account hashes, all six demo logins, and idempotence. The ordinary `mvnw.cmd test` suite runs `DemoDataSeederTest` against isolated H2 and does not write to MySQL.

These accounts are intended only for local development. Remove or disable them before connecting this database to a public deployment.
