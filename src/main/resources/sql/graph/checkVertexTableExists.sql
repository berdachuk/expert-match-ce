SELECT EXISTS (
    SELECT FROM information_schema.tables
    WHERE table_schema = 'ag_catalog'
    AND table_name = '{tableName}'
)