#!/bin/sh
set -eu

MVNW_REPOURL="${MVNW_REPOURL:-https://repo.maven.apache.org/maven2}"
MVNW_VERSION="${MVNW_VERSION:-3.9.16}"
MVNW_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MVNW_VERSION}"
MVNW_BIN="${MVNW_HOME}/apache-maven-${MVNW_VERSION}/bin/mvn"

if [ ! -x "${MVNW_BIN}" ]; then
  mkdir -p "${MVNW_HOME}"
  ARCHIVE="${MVNW_HOME}/apache-maven-${MVNW_VERSION}-bin.tar.gz"
  URL="${MVNW_REPOURL}/org/apache/maven/apache-maven/${MVNW_VERSION}/apache-maven-${MVNW_VERSION}-bin.tar.gz"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "${URL}" -o "${ARCHIVE}"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "${URL}" -O "${ARCHIVE}"
  else
    echo "Se requiere curl o wget para descargar Maven ${MVNW_VERSION}." >&2
    exit 1
  fi
  tar -xzf "${ARCHIVE}" -C "${MVNW_HOME}"
  rm -f "${ARCHIVE}"
fi

exec "${MVNW_BIN}" "$@"
