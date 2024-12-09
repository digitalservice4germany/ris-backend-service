#!/bin/bash

set -eu

readonly DEBUG="${DEBUG:-unset}"
if [ "${DEBUG}" != unset ]; then
  set -x
fi

_fail() {
  printf "\033[0;31m==> %s\033[0m\n\n" "$1"
}

_success() {
  printf "\033[0;32m==> %s\033[0m\n\n" "$1"
}

_info() {
  printf "\033[1;33m==> %s\033[0m\n\n" "$1"
}

_user() {
  printf "\033[0;33m%s\033[0m" "$1"
}

_setup_git_hooks() {
  _user "Do you want to install the Git hooks ? (y/n) "
  read -r answer
  if [ "$answer" = "y" ]; then

    if ! command -v lefthook > /dev/null 2>&1; then
      _fail "Setup requires Lefthook, please install first"
      exit 1
    fi
    lefthook install
    _info "Git hooks installed.."
  fi
}

_setup_direnv() {
  # Allow direnv to use `.env` files as described here:
  # https://github.com/direnv/direnv/blob/master/man/direnv.toml.1.md#codeloaddotenvcode
  [ -d ~/.config/direnv/. ] || mkdir ~/.config/direnv
  cat > ~/.config/direnv/direnv.toml<< EOF
[global]
load_dotenv = true
EOF

}

_init() {
  _setup_git_hooks
  _setup_direnv
}

_start() {
  cd ./backend/
  gradle --stop
  gradle compileJava --build-cache --continuous --quiet &
  gradle bootRun --build-cache
}

_env() {
  if ! command -v gopass > /dev/null 2>&1; then
    fail "Setup requires gopass, please install first"
    exit 1
  fi

  cat > ./.env<< EOF
GH_PACKAGES_REPOSITORY_USER=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-juris-xml-export/username)
GH_PACKAGES_REPOSITORY_TOKEN=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-juris-xml-export/token)
OAUTH2_CLIENT_ISSUER=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-dev-oauth2-client/issuer-uri)
OAUTH2_CLIENT_ID=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-dev-oauth2-client/client-id)
OAUTH2_CLIENT_SECRET=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-dev-oauth2-client/client-secret)
E2E_TEST_USER=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-e2e-test-user/username)
E2E_TEST_PASSWORD=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-e2e-test-user/password)
E2E_TEST_USER_BGH=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-e2e-test-user-bgh/username)
E2E_TEST_PASSWORD_BGH=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-e2e-test-user-bgh/password)
E2E_TEST_USER_EXTERNAL=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-e2e-test-user-external/username)
E2E_TEST_PASSWORD_EXTERNAL=$(gopass show -o -y neuris/maven.pkg.github.com/digitalservicebund/neuris-e2e-test-user-external/password)
MY_UID=$(id -u)
MY_GID=$(id -g)
DB_URL=jdbc:postgresql://localhost:5432/postgres
DB_USER=test
DB_PASSWORD=test
EOF

  if ! command -v direnv > /dev/null 2>&1; then
    # Direnv is not installed. Source the .env file manually
    set -o allexport
    source .env
    set +o allexport
  else
    direnv allow .
  fi
}

_dev() {
  if ! command -v docker > /dev/null 2>&1; then
    _fail "Dev requires docker, please install first"
    exit 1
  fi
  docker build ./frontend -f frontend/Dockerfile -t neuris/frontend

  wait=""
  services=""
  for arg in "$@"; do
    case $arg in
      -n|--no-backend)
        services="traefik redis postgres14 frontend"
        ;;
      -d|--detached)
        wait="--wait"
        ;;
    esac
  done
  docker compose up $wait $services

  echo "The application is available at http://127.0.0.1"
}

_down() {
  docker compose stop
}

_commit_message_template() {
  template="$PWD/.git/.gitmessage.txt"
  git config commit.template "$template"
  cat << EOF > "$template"
Subject

Some context/description

Addresses RISDEV-${1#RISDEV-}
EOF
}

_clean_staging() {
  _user "Application url? " && read -r APP_URL
  _user "Session ID? " && read -r SESSION_ID

  endpoint="https://${APP_URL#"https://"}/api/v1/caselaw/documentunits"
  regex="\"uuid\":\"[a-z0-9-]\{36\}\""

  documentUnits=$(curl -s -H "cookie: SESSION=$SESSION_ID" "$endpoint" \
    | grep -o "$regex" \
    | sed "s/\"uuid\":\"//g" \
    | sed "s/\"//g")

  n_deleted=0
  for documentUnit in $documentUnits; do
    curl -X DELETE -H "cookie: SESSION=$SESSION_ID" "$endpoint"/"$documentUnit"
    n_deleted=$((n_deleted + 1))
  done

  _info "$n_deleted documentunits deleted."
}

_help() {
  echo "Usage: ./run.sh [command]"
  echo ""
  echo "Available commands:"
  echo "init                  Set up repository for development"
  echo "env                   Provide shell env build/test tooling"
  echo "dev                   Start full-stack development environment"
  echo "                      Add '-' or '--no-backend' to start backend separately"
  echo "                      Add '-d' or '--detached' to check the health of the services in the background instead of showing the log stream"
  echo "down                  Stop development environment"
  echo "clean-staging         Deletes all existing documentunits on staging"
  echo "cm <issue-number>     Configure commit message template with given issue number;"
  echo "                      issue number can be with or without prefix: 1234, RISDEV-1234."
}

cmd="${1:-}"
case "$cmd" in
  "init") _init ;;
  "env") _env ;;
  "dev")
    shift
    _dev "$@";;
  "down") _down ;;
  "clean-staging") _clean_staging ;;
  "_start") _start ;;
  "cm")
    shift
    _commit_message_template "$@"
    ;;
  *) _help ;;
esac
