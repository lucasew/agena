#!/bin/bash
set -e

# Run SQLC generation
sqlc generate

# Remove rogue file if it exists
rm -f app/src/main/java/Queries.java

# Apply Android patch to generated Queries.java
if [ -f "app/src/main/java/com/biglucas/agena/db/Queries.java" ]; then
    sed -i 's/results.getObject(2, OffsetDateTime.class)/((com.biglucas.agena.db.wrapper.AndroidResultSet)results).getObject(2, OffsetDateTime.class)/g' app/src/main/java/com/biglucas/agena/db/Queries.java
fi
