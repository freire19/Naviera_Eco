#!/bin/bash
# ============================================================================
# Naviera Desktop -- Build & Package Script (Linux)
# Requires: Liberica Full JDK 17 (with JavaFX) at /opt/liberica-full-jdk-17
# ============================================================================
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# Liberica Full JDK 17 (includes JavaFX + all Java modules)
LIBERICA_HOME="${LIBERICA_HOME:-/opt/liberica-full-jdk-17}"
JAVA="$LIBERICA_HOME/bin/java"
JAVAC="$LIBERICA_HOME/bin/javac"
JAR_CMD="$LIBERICA_HOME/bin/jar"
JPACKAGE="$LIBERICA_HOME/bin/jpackage"

# JavaFX is built into Liberica Full — get module path from jmods
JAVAFX_JMODS="$LIBERICA_HOME/jmods"

# Source and output directories
SRC_DIR="$PROJECT_ROOT/src"
RESOURCES_DIR="$PROJECT_ROOT/resources"
LIB_DIR="$PROJECT_ROOT/lib"
BUILD_DIR="$PROJECT_ROOT/build"
CLASSES_DIR="$BUILD_DIR/classes"
DIST_DIR="$BUILD_DIR/dist"
INSTALLER_DIR="$BUILD_DIR/installer"

# Application metadata
APP_NAME="Naviera"
APP_VERSION="1.0.0"
APP_VENDOR="Naviera Eco"
APP_DESCRIPTION="Sistema de Gestao Naviera"
JAR_NAME="naviera-desktop.jar"
MAIN_CLASS="gui.Launch"

# Icon (PNG for Linux)
ICON_FILE="$SRC_DIR/gui/icons/logo_icon.png"

# All modules needed in the runtime (JavaFX + Java platform)
ALL_MODULES="javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web,javafx.swing,javafx.media"
ALL_MODULES="$ALL_MODULES,java.base,java.sql,java.sql.rowset,java.desktop,java.logging,java.naming"
ALL_MODULES="$ALL_MODULES,java.transaction.xa,java.net.http,java.security.jgss,java.xml,java.prefs"
ALL_MODULES="$ALL_MODULES,java.datatransfer,java.scripting,java.compiler,java.management"
ALL_MODULES="$ALL_MODULES,jdk.unsupported,jdk.unsupported.desktop,jdk.jsobject,jdk.xml.dom"

# ---------------------------------------------------------------------------
# Helper functions
# ---------------------------------------------------------------------------
info()  { echo -e "\033[1;34m[INFO]\033[0m  $*"; }
ok()    { echo -e "\033[1;32m[OK]\033[0m    $*"; }
err()   { echo -e "\033[1;31m[ERROR]\033[0m $*" >&2; }
die()   { err "$@"; exit 1; }

# ---------------------------------------------------------------------------
# Pre-flight checks
# ---------------------------------------------------------------------------
info "Checking prerequisites..."

[ -x "$JAVA" ]     || die "Liberica Full JDK 17 not found at $LIBERICA_HOME. Install with: wget + tar to /opt/"
[ -x "$JAVAC" ]    || die "javac not found in Liberica JDK"
[ -x "$JPACKAGE" ] || die "jpackage not found in Liberica JDK"

JAVA_VERSION=$("$JAVA" -version 2>&1 | head -1 | grep -oP '"(\d+)' | tr -d '"')
ok "Liberica Full JDK $JAVA_VERSION detected"

# Verify JavaFX is built-in
if ! "$JAVA" --list-modules 2>/dev/null | grep -q "javafx.base"; then
    die "JavaFX not found in JDK. Use Liberica FULL JDK (not standard)."
fi
ok "JavaFX modules built into JDK"

# Verify java.sql is available
if ! "$JAVA" --list-modules 2>/dev/null | grep -q "java.sql@"; then
    die "java.sql not found in JDK — broken installation"
fi
ok "java.sql module available"

# ---------------------------------------------------------------------------
# Step 0: Clean previous build
# ---------------------------------------------------------------------------
info "Cleaning previous build..."
rm -rf "$CLASSES_DIR" "$DIST_DIR" "$INSTALLER_DIR"
mkdir -p "$CLASSES_DIR" "$DIST_DIR" "$INSTALLER_DIR"
ok "Build directories created"

# ---------------------------------------------------------------------------
# Step 1: Build classpath from lib/ JARs
# ---------------------------------------------------------------------------
info "Building classpath..."
LIB_CP=""
for jar in "$LIB_DIR"/*.jar; do
    [ -f "$jar" ] || continue
    LIB_CP="${LIB_CP:+$LIB_CP:}$jar"
done
ok "Classpath ready ($(echo "$LIB_CP" | tr ':' '\n' | wc -l) lib JARs)"

# ---------------------------------------------------------------------------
# Step 2: Compile all .java files
# ---------------------------------------------------------------------------
info "Compiling sources..."

JAVA_FILES=$(find "$SRC_DIR" -name "*.java" -type f)
FILE_COUNT=$(echo "$JAVA_FILES" | wc -l)
info "  Found $FILE_COUNT .java files"

# Liberica Full has JavaFX in the JDK itself — no external module-path needed
"$JAVAC" \
    --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.swing,javafx.media \
    -cp "$LIB_CP" \
    -d "$CLASSES_DIR" \
    -encoding UTF-8 \
    -source 17 -target 17 \
    $JAVA_FILES

ok "Compilation complete"

# ---------------------------------------------------------------------------
# Step 3: Copy resources to classes directory
# ---------------------------------------------------------------------------
info "Copying resources..."

(cd "$SRC_DIR" && find . -name "*.fxml" -exec cp --parents {} "$CLASSES_DIR/" \;) 2>/dev/null && \
    ok "  FXML files copied" || true

(cd "$SRC_DIR" && find . -name "*.css" -exec cp --parents {} "$CLASSES_DIR/" \;) 2>/dev/null && \
    ok "  CSS files copied" || true

(cd "$SRC_DIR" && find . -name "*.png" -exec cp --parents {} "$CLASSES_DIR/" \;) 2>/dev/null && \
    ok "  PNG icons copied" || true

if [ -d "$RESOURCES_DIR" ]; then
    cp -r "$RESOURCES_DIR"/* "$CLASSES_DIR/" 2>/dev/null && \
        ok "  resources/ directory copied" || true
fi

for cfg in db.properties db.properties.example impressoras.config sync_config.properties; do
    if [ -f "$PROJECT_ROOT/$cfg" ]; then
        cp "$PROJECT_ROOT/$cfg" "$CLASSES_DIR/"
    fi
done
ok "Resources copied"

# ---------------------------------------------------------------------------
# Step 4: Cleanup stale paths
# ---------------------------------------------------------------------------
find "$CLASSES_DIR" -maxdepth 1 -name "home" -type d -exec rm -rf {} + 2>/dev/null || true
if [ -d "$CLASSES_DIR/src" ]; then
    cp -r "$CLASSES_DIR/src/"* "$CLASSES_DIR/" 2>/dev/null || true
    rm -rf "$CLASSES_DIR/src"
fi
ok "Resource paths verified"

# ---------------------------------------------------------------------------
# Step 5: Create JAR with manifest
# ---------------------------------------------------------------------------
info "Creating JAR..."

MANIFEST_CP=""
for jar in "$LIB_DIR"/*.jar; do
    [ -f "$jar" ] || continue
    MANIFEST_CP="${MANIFEST_CP:+$MANIFEST_CP }lib/$(basename "$jar")"
done

MANIFEST_FILE="$BUILD_DIR/MANIFEST.MF"
{
    echo "Manifest-Version: 1.0"
    echo "Main-Class: $MAIN_CLASS"
    LINE="Class-Path:"
    for entry in $MANIFEST_CP; do
        if [ ${#LINE} -gt 0 ] && [ $(( ${#LINE} + 1 + ${#entry} )) -gt 70 ]; then
            echo "$LINE"
            LINE=" $entry"
        else
            LINE="$LINE $entry"
        fi
    done
    [ -n "$LINE" ] && echo "$LINE"
    echo ""
} > "$MANIFEST_FILE"

"$JAR_CMD" cfm "$DIST_DIR/$JAR_NAME" "$MANIFEST_FILE" -C "$CLASSES_DIR" .
ok "JAR created: $DIST_DIR/$JAR_NAME"

mkdir -p "$DIST_DIR/lib"
cp "$LIB_DIR"/*.jar "$DIST_DIR/lib/"
ok "Dependencies copied to dist/"

# ---------------------------------------------------------------------------
# Step 6: Create native installer with jpackage
# ---------------------------------------------------------------------------
info "Creating native installer with jpackage..."

# Determine installer type
case "$(uname -s)" in
    Linux*)
        if command -v dpkg >/dev/null 2>&1; then
            PKG_TYPE="deb"
        elif command -v rpm >/dev/null 2>&1; then
            PKG_TYPE="rpm"
        else
            PKG_TYPE="app-image"
        fi
        ;;
    Darwin*) PKG_TYPE="dmg" ;;
    *)       PKG_TYPE="app-image" ;;
esac

info "  Installer type: $PKG_TYPE"

# Prepare input directory
JPACKAGE_INPUT="$BUILD_DIR/jpackage-input"
rm -rf "$JPACKAGE_INPUT"
mkdir -p "$JPACKAGE_INPUT/lib"
cp "$DIST_DIR/$JAR_NAME" "$JPACKAGE_INPUT/"
cp "$LIB_DIR"/*.jar "$JPACKAGE_INPUT/lib/"

# Bundle database_scripts for the setup wizard (migrations)
if [ -d "$PROJECT_ROOT/database_scripts" ]; then
    cp -r "$PROJECT_ROOT/database_scripts" "$JPACKAGE_INPUT/database_scripts"
    ok "  database_scripts/ bundled for setup wizard"
fi

# Bundle relatorios (JasperReports)
if [ -d "$PROJECT_ROOT/relatorios" ]; then
    cp -r "$PROJECT_ROOT/relatorios" "$JPACKAGE_INPUT/relatorios"
    ok "  relatorios/ bundled"
fi

# Icon
JPACKAGE_ICON_OPT=""
if [ -f "$ICON_FILE" ]; then
    JPACKAGE_ICON_OPT="--icon $ICON_FILE"
    info "  Using icon: $ICON_FILE"
fi

# Linux .deb options
LINUX_OPTS=""
if [ "$PKG_TYPE" = "deb" ]; then
    LINUX_OPTS="--linux-deb-maintainer suporte@naviera.com.br"
    LINUX_OPTS="$LINUX_OPTS --linux-app-category utilities"
    LINUX_OPTS="$LINUX_OPTS --linux-shortcut"
    LINUX_OPTS="$LINUX_OPTS --linux-package-deps libgtk-3-0"
fi

# jpackage uses Liberica Full JDK — JavaFX + java.sql are built-in
# No external module-path needed, no post-build patching
"$JPACKAGE" \
    --input "$JPACKAGE_INPUT" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --vendor "$APP_VENDOR" \
    --description "$APP_DESCRIPTION" \
    --dest "$INSTALLER_DIR" \
    --type "$PKG_TYPE" \
    --add-modules "$ALL_MODULES" \
    --java-options "--add-opens javafx.base/com.sun.javafx.reflect=ALL-UNNAMED" \
    --java-options "--add-opens javafx.graphics/com.sun.javafx.css=ALL-UNNAMED" \
    --java-options "--add-opens javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED" \
    --java-options "--add-opens javafx.graphics/com.sun.javafx.scene.layout=ALL-UNNAMED" \
    --java-options "--add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED" \
    --java-options "--add-opens javafx.controls/javafx.scene.control.skin=ALL-UNNAMED" \
    --java-options "-Dfile.encoding=UTF-8" \
    $JPACKAGE_ICON_OPT \
    $LINUX_OPTS

ok "Native installer created"

# ---------------------------------------------------------------------------
# Step 7: Verify .deb contents
# ---------------------------------------------------------------------------
if [ "$PKG_TYPE" = "deb" ]; then
    DEB_FILE=$(ls "$INSTALLER_DIR"/*.deb 2>/dev/null | head -1)
    if [ -n "$DEB_FILE" ]; then
        info "Verifying .deb contents..."
        VERIFY_DIR=$(mktemp -d)
        dpkg-deb -x "$DEB_FILE" "$VERIFY_DIR"

        # Check JavaFX native libs
        SO_COUNT=$(find "$VERIFY_DIR" -name "libglass*.so" -o -name "libprism*.so" 2>/dev/null | wc -l)
        if [ "$SO_COUNT" -gt 0 ]; then
            ok "  JavaFX native libs present ($SO_COUNT .so files)"
        else
            err "  JavaFX native libs MISSING — installer may not work"
        fi

        # Check java.sql module
        if find "$VERIFY_DIR" -path "*/legal/java.sql" -type d 2>/dev/null | grep -q "."; then
            ok "  java.sql module present"
        else
            err "  java.sql module MISSING"
        fi

        rm -rf "$VERIFY_DIR"
    fi
fi

ls -lh "$INSTALLER_DIR/"

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
info "============================================"
info "  Build complete!"
info "============================================"
info "  JAR:       $DIST_DIR/$JAR_NAME"
info "  Installer: $INSTALLER_DIR/"
info "  JDK:       $LIBERICA_HOME"
info ""
info "  To install:"
info "    sudo dpkg -i $INSTALLER_DIR/*.deb"
echo ""
