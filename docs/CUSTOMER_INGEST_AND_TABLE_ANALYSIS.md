# Customer Information During Ingest and Customer Table Option

## Current State: How Customer Is Extracted and Used

### Source: `work_experience_json`

The external table `work_experience.work_experience_json` can contain:

- **`customer`** (jsonb) – Customer object (id, name, industry, etc.)
- **`customer_description`** (text) – Description of the customer/company
- **`customer_name`** (text) – Top-level customer name (legacy/flat schema)

### Extraction (DatabaseIngestionServiceImpl)

Customer data is now taken from both the JSONB object and top-level columns:

1. **Customer map**  
   Keys tried in order: `customer`, `customer_data`, `customer_json`.  
   Handles PostgreSQL JSONB (PGobject) and Map.

2. **Customer ID**  
   From customer map: `id`, then `customer_id`.  
   Persisted on `work_experience.customer_id` when present (e.g. external system id).

3. **Customer name**  
   First non-blank of:
    - Customer map: `name`, `customerName`
    - Record: `customer_name`, `customer` (string column)

4. **Customer description**  
   From record: `customer_description`.  
   Used in work experience metadata (`customer_description` in JSON) when present; otherwise a generated description
   from customer name is used.

5. **Industry**  
   First non-blank of:
    - Customer map: `industry`
    - Record: `industry`, `customer_description`  
      So `customer_description` can still act as industry fallback when there is no dedicated industry field.

### Downstream Use

- **ProjectData**
    - `customerId` – from JSONB customer or null
    - `customerName` – from JSONB + `customer_name` + string `customer`
    - `customerDescription` – from `customer_description`
    - `industry` – from customer map / record / `customer_description` fallback

- **WorkExperience**
    - `customerId` and `customerName` are stored on `work_experience` (table already had `customer_id`,
      `customer_name`).
    - `createOrUpdate` now includes `customer_id` in INSERT/UPDATE.

- **Metadata**
    - `customer_description` in work experience metadata uses `project.customerDescription()` when set, otherwise a
      generated description from customer name.

- **Graph**
    - Customer vertices and relationships are built from `work_experience` (e.g. `findAllCustomers`,
      `findAllExpertCustomerRelationships`).
    - When `customer_id` is null, the graph uses `COALESCE(customer_id, 'CUSTOMER_' || customer_name)` so customers are
      still represented.

## Do We Need a Customer Table?

### Current Model (No Dedicated Customer Table)

- Customer is **denormalized**: `customer_id` and `customer_name` live on `work_experience` (and optionally on `project`
  in the domain, but the project table does not store `customer_id` in the current schema).
- “Customers” are derived from work experience rows (e.g. `findAllCustomers`).
- Same logical customer can appear many times (one per work experience row) with the same or different `customer_id`/
  `customer_name`.

### When a Customer Table Helps

A dedicated **customer** table is useful if you need to:

1. **Normalize customer identity**  
   One row per customer (e.g. by external `customer_id` or by normalized name), and link work experience (and projects)
   to it via `customer_id`.

2. **Store extra customer attributes**  
   e.g. `customer_description`, industry, segment, contact, address, etc., in one place instead of only in metadata or
   derived from first-seen row.

3. **Manage customer master data**  
   Create/update customers during ingest (find-or-create by `customer_id` or name), and reuse the same customer for
   multiple work experiences and projects.

4. **Query/report by customer**  
   Simple queries and joins on a single customer table instead of aggregating from `work_experience`.

### Possible Design (If You Add a Customer Table)

- **Table**  
  e.g. `expertmatch.customer` with at least:  
  `id` (PK), `name`, optional `description`, `industry`, external `source_id`, timestamps.

- **Ingest**
    - Resolve customer from `work_experience_json` (JSONB `customer` + `customer_description` + `customer_name`) as
      today.
    - Find-or-create customer by external id or name, then set `work_experience.customer_id` to the table’s `id`.

- **Project / work_experience**
    - Keep `work_experience.customer_id` (and optionally `project.customer_id`) as FK to `customer.id` once the table
      exists.

Current code already passes through and persists **customer_id** and **customer_name** from the external source; adding
a customer table would be a normalization step on top of this without changing the ingest contract for `customer` and
`customer_description`.
