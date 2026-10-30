# Product Lifecycle and Catalogue Management System

A Java Swing desktop application with PostgreSQL/JDBC for a student capstone project. It demonstrates five modules:
1. Product Management
2. Category Management
3. Sales Channel Management
4. Product Lifecycle Management
5. Inventory Management

## Requirements
- JDK 17 or later
- PostgreSQL 14 or later (other recent versions should work)
- Maven 3.8+
- PostgreSQL JDBC driver (Maven downloads it automatically)

## 1. Create the database
Open `psql` or pgAdmin and run:

```sql
CREATE DATABASE plcm_db;
```

Connect to `plcm_db`, then execute the SQL in `database/schema.sql`.

Example with `psql`:

```bash
psql -U postgres -d plcm_db -f database/schema.sql
```

## 2. Configure database credentials
The app reads these environment variables:
- `DB_URL` (default: `jdbc:postgresql://localhost:5432/plcm_db`)
- `DB_USER` (default: `postgres`)
- `DB_PASSWORD` (default: empty string)

Set them to match your PostgreSQL installation. Do not put real passwords in source code or commit them to GitHub.

Windows PowerShell example (for the current terminal only):

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/plcm_db"
$env:DB_USER="postgres"
$env:DB_PASSWORD="your_postgres_password"
```

## 3. Run the application
From the project root:

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="edu.niet.plcm.Main"
```

If `mvn exec:java` says the exec plugin is unavailable, use:

```bash
mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt
java -cp "target/classes;$(Get-Content classpath.txt)" edu.niet.plcm.Main
```

On Windows, Maven's exec plugin can also be added to `pom.xml`; the simple recommended option is to run from an IDE:
1. Open the folder as a Maven project in IntelliJ IDEA or Eclipse.
2. Wait for Maven dependencies to load.
3. Set environment variables in the run configuration.
4. Run `edu.niet.plcm.Main`.

## How to use
- **Products:** Add, update, delete and view product records. SKU, name and non-negative price are required. Category ID is optional but must refer to an existing category if entered.
- **Categories:** Add, update, delete and view categories.
- **Sales Channels:** Add, update, delete and view channels.
- **Lifecycle:** Select a product and apply an allowed lifecycle transition. The app records each change in `lifecycle_history`.
- **Inventory:** Enter an existing product ID, quantity, and reorder level. Quantities cannot be negative.

Create categories and sales channels before associating them with products. Product-channel relationships are stored in `product_channels`; this starter UI defines the schema but does not yet include a screen to create those mappings.

## Test plan
1. Add a category, e.g. `Electronics`.
2. Add a sales channel, e.g. `Company Website`.
3. Add a product with a unique SKU, name, non-negative price, and category ID.
4. Refresh and verify the product appears in the table.
5. Select the product, change its details, and update it.
6. Try a negative price and confirm validation rejects it.
7. In Lifecycle, move the product from `DRAFT` to `REVIEW`, then to `PUBLISHED`. Check `lifecycle_history` in PostgreSQL.
8. In Inventory, set quantity and reorder level; try a negative quantity and confirm validation rejects it.
9. Try a duplicate SKU or duplicate category name and confirm PostgreSQL rejects the duplicate.
10. Delete test data only after checking that you no longer need it.

## Known limitations / next steps
- The starter application uses direct JDBC calls inside Swing panels for clarity; a larger version should separate UI, service, and DAO layers.
- The schema supports product-to-channel mapping, but the UI for assigning products to channels remains to be implemented.
- There is no authentication, authorization, automated marketplace synchronization, image upload, or audit dashboard.
- Run and test the application in your own environment before reporting that a test passed.

## Appendix for the NIET report
Include a short description of each module, the schema diagram or table relationships, screenshots of the running application, test cases with actual results, and a repository link. Do not paste the entire codebase into the report if it exceeds the 10-page capstone appendix limit; include key commented excerpts and link to the complete repository.
