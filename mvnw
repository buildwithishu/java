#!/usr/bin/env sh
set -eu

MAVEN_VERSION="${MAVEN_VERSION:-3.9.16}"
BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MAVEN_DIR="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"
MAVEN_BIN="$MAVEN_DIR/bin/mvn"
ARCHIVE="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION-bin.tar.gz"
ARCHIVE_PART="$ARCHIVE.part"
CHECKSUM="$ARCHIVE.sha512"
PRIMARY_URL="https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
FALLBACK_URL="https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "$MAVEN_BIN" ]; then
  mkdir -p "$BASE_DIR/.mvn"
  if [ ! -f "$ARCHIVE" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl --connect-timeout 20 --max-time 300 -fsSL "$PRIMARY_URL" -o "$ARCHIVE_PART" \
        || curl --connect-timeout 20 --max-time 300 -fsSL "$FALLBACK_URL" -o "$ARCHIVE_PART"
      curl --connect-timeout 20 --max-time 60 -fsSL "$PRIMARY_URL.sha512" -o "$CHECKSUM" \
        || curl --connect-timeout 20 --max-time 60 -fsSL "$FALLBACK_URL.sha512" -o "$CHECKSUM"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$PRIMARY_URL" -O "$ARCHIVE_PART" || wget -q "$FALLBACK_URL" -O "$ARCHIVE_PART"
      wget -q "$PRIMARY_URL.sha512" -O "$CHECKSUM" || wget -q "$FALLBACK_URL.sha512" -O "$CHECKSUM"
    else
      echo "curl or wget is required to download Maven $MAVEN_VERSION" >&2
      exit 1
    fi
    EXPECTED_CHECKSUM="$(awk '{print $1}' "$CHECKSUM")"
    ACTUAL_CHECKSUM="$(sha512sum "$ARCHIVE_PART" | awk '{print $1}')"
    if [ "$EXPECTED_CHECKSUM" != "$ACTUAL_CHECKSUM" ]; then
      echo "Maven archive checksum verification failed" >&2
      exit 1
    fi
    mv "$ARCHIVE_PART" "$ARCHIVE"
  fi
  tar -xzf "$ARCHIVE" -C "$BASE_DIR/.mvn"
fi

exec "$MAVEN_BIN" "$@"
