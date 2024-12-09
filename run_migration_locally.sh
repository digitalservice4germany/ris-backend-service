# Bash script to run migration locally for the use of backend service

# Set the environment variables
export RIS_MIGRATION_TABLES_LOCATION=juris-xml-data
export RIS_MIGRATION_INCLUDE_NORM_ABBREVIATIONS=true
export RIS_MIGRATION_CLI_MODE=true

# database config
export RIS_MIGRATION_DB_HOST=localhost
export RIS_MIGRATION_DB_PORT=5432
export RIS_MIGRATION_DB_NAME=neuris
export RIS_MIGRATION_DB_USER=migration
export RIS_MIGRATION_DB_PASSWORD=migration
export RIS_MIGRATION_DB_SCHEMA=incremental_migration

DATA_MIGRATION_SERVICE_PATH="../ris-data-migration"
DATA_MIGRATION_IMPORT_PATH="./juris-xml-data"

#  In ris-backend-service restart the database to add the user and the scheme:
docker compose -f "compose.yaml" up postgres14 -d
echo "Docker run in background, waiting"
sleep 15

if [ -d "$DATA_MIGRATION_SERVICE_PATH" ]; then
  # Move to migration service
  cd "$DATA_MIGRATION_SERVICE_PATH"  || exit
  direnv allow
  echo "Moved to $DATA_MIGRATION_SERVICE_PATH"
else
  echo "Folder does not exist, are you missing the migration folder? "
  exit 1
fi

# Remove the juris data folder and recreate and empty one
if [ -d "$DATA_MIGRATION_IMPORT_PATH" ]; then
  rm -rf "$DATA_MIGRATION_IMPORT_PATH"
  mkdir "$DATA_MIGRATION_IMPORT_PATH"
  echo "Prepaid folder for juris reimport: $DATA_MIGRATION_IMPORT_PATH"
fi

# Check if you can access the right bucket with
aws s3 ls --profile otc --endpoint-url https://obs.eu-de.otc.t-systems.com s3://neuris-migration-juris-data
# Download the lookup tables
aws s3 cp --profile otc --endpoint-url https://obs.eu-de.otc.t-systems.com --recursive s3://neuris-migration-juris-data/monthly/2024/10/Tabellen ./juris-xml-data/Tabellen
# Download BGH DocumentationUnits
aws s3 cp --profile otc --endpoint-url https://obs.eu-de.otc.t-systems.com --recursive s3://neuris-migration-juris-data/monthly/2024/05/BGH-juris/RSP/2022/ ./juris-xml-data/BGH-juris/RSP/2022/


#  For console logging
export SPRING_PROFILES_ACTIVE=dev

# Build the ris-data-migration application into a jar
./gradlew :cli:bootJar

# Build the ris-data-migration application into a jar
java -jar cli/build/libs/ris-data-migration-cli.jar refdata seed

# Import the static lookup tables into your new schema (see Confluence "Wertetabellen" to find out what is static and dynamic)
java -jar cli/build/libs/ris-data-migration-cli.jar juris-table seed

# Import the BGH Documentation Units
java -jar cli/build/libs/ris-data-migration-cli.jar juris-r migrate -p juris-xml-data/

