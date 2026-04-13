#!/bin/bash
# ============================================================================
# Naviera Desktop -- Build & Package Script (Linux)
# Requires: JDK 17+, JavaFX SDK 23.0.2+
# ============================================================================
set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration -- adjust these paths for your environment
# ---------------------------------------------------------------------------
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# JavaFX SDK location (default: /opt/javafx-sdk-23.0.2)
JAVAFX_PATH="${JAVAFX_PATH:-/opt/javafx-sdk-23.0.2/lib}"

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

# Icon (PNG for Linux, will be used by jpackage)
ICON_FILE="$SRC_DIR/gui/icons/logo_icon.png"

# JavaFX modules required
JAVAFX_MODULES="javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.web,javafx.swing,javafx.media"

# Java platform modules required by the app (java.sql for DB, java.desktop for AWT/print/sound, etc.)
JAVA_MODULES="java.base,java.sql,java.desktop,java.logging,java.naming,java.net.http,java.security.jgss,java.xml,java.prefs,java.datatransfer,java.scripting,java.management,jdk.unsupported"

# All modules for jpackage runtime
ALL_MODULES="$JAVAFX_MODULES,$JAVA_MODULES"

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

command -v java  >/dev/null 2>&1 || die "java not found in PATH"
command -v javac >/dev/null 2>&1 || die "javac not found in PATH"

JAVA_VERSION=$(java -version 2>&1 | head -1 | grep -oP '\"(\d+)' | tr -d '"')
if [ "$JAVA_VERSION" -lt 17 ] 2>/dev/null; then
    die "JDK 17+ required (found version $JAVA_VERSION)"
fi
ok "JDK $JAVA_VERSION detected"

if [ ! -d "$JAVAFX_PATH" ]; then
    die "JavaFX SDK not found at $JAVAFX_PATH. Set JAVAFX_PATH env variable."
fi
ok "JavaFX SDK found at $JAVAFX_PATH"

# ---------------------------------------------------------------------------
# Step 0: Clean previous build
# ---------------------------------------------------------------------------
info "Cleaning previous build..."
rm -rf "$CLASSES_DIR" "$DIST_DIR" "$INSTALLER_DIR"
mkdir -p "$CLASSES_DIR" "$DIST_DIR" "$INSTALLER_DIR"
ok "Build directories created"

# ---------------------------------------------------------------------------
# Step 1: Build classpath from lib/ JARs + JavaFX JARs
# ---------------------------------------------------------------------------
info "Building classpath..."
LIB_CP=""
for jar in "$LIB_DIR"/*.jar; do
    [ -f "$jar" ] || continue
    LIB_CP="${LIB_CP:+$LIB_CP:}$jar"
done

JAVAFX_CP=""
for jar in "$JAVAFX_PATH"/*.jar; do
    [ -f "$jar" ] || continue
    JAVAFX_CP="${JAVAFX_CP:+$JAVAFX_CP:}$jar"
done

FULL_CP="$JAVAFX_CP:$LIB_CP"
ok "Classpath ready ($(echo "$LIB_CP" | tr ':' '\n' | wc -l) lib JARs)"

# ---------------------------------------------------------------------------
# Step 2: Compile all .java files
# ---------------------------------------------------------------------------
info "Compiling sources..."

# Collect all .java files
JAVA_FILES=$(find "$SRC_DIR" -name "*.java" -type f)
FILE_COUNT=$(echo "$JAVA_FILES" | wc -l)
info "  Found $FILE_COUNT .java files"

# Compile with module path for JavaFX and classpath for other libs
javac \
    --module-path "$JAVAFX_PATH" \
    --add-modules "$JAVAFX_MODULES" \
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

# Copy FXML, CSS, PNG from src/ preserving package structure (relative to src/)
(cd "$SRC_DIR" && find . -name "*.fxml" -exec cp --parents {} "$CLASSES_DIR/" \;) 2>/dev/null && \
    ok "  FXML files copied from src/" || true

(cd "$SRC_DIR" && find . -name "*.css" -exec cp --parents {} "$CLASSES_DIR/" \;) 2>/dev/null && \
    ok "  CSS files copied from src/" || true

(cd "$SRC_DIR" && find . -name "*.png" -exec cp --parents {} "$CLASSES_DIR/" \;) 2>/dev/null && \
    ok "  PNG icons copied from src/" || true

# Copy resources/ directory contents (css/, icons/)
if [ -d "$RESOURCES_DIR" ]; then
    cp -r "$RESOURCES_DIR"/* "$CLASSES_DIR/" 2>/dev/null && \
        ok "  resources/ directory copied" || true
fi

# Copy config files that the app may need at runtime
for cfg in db.properties db.properties.example impressoras.config sync_config.properties; do
    if [ -f "$PROJECT_ROOT/$cfg" ]; then
        cp "$PROJECT_ROOT/$cfg" "$CLASSES_DIR/"
    fi
done
ok "Resources copied"

# ---------------------------------------------------------------------------
# Step 4: Cleanup stale paths (if any)
# ---------------------------------------------------------------------------
# Remove any absolute-path artifacts from previous builds
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

# Build Class-Path for manifest (all lib JARs, relative to jar location)
MANIFEST_CP=""
for jar in "$LIB_DIR"/*.jar; do
    [ -f "$jar" ] || continue
    MANIFEST_CP="${MANIFEST_CP:+$MANIFEST_CP }lib/$(basename "$jar")"
done
for jar in "$JAVAFX_PATH"/*.jar; do
    [ -f "$jar" ] || continue
    MANIFEST_CP="${MANIFEST_CP:+$MANIFEST_CP }javafx/$(basename "$jar")"
done

# Create manifest (Class-Path must wrap at 72 chars with leading space continuation)
MANIFEST_FILE="$BUILD_DIR/MANIFEST.MF"
{
    echo "Manifest-Version: 1.0"
    echo "Main-Class: $MAIN_CLASS"
    # Write Class-Path with proper line continuation (72 char limit per line)
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

# Create JAR
jar cfm "$DIST_DIR/$JAR_NAME" "$MANIFEST_FILE" -C "$CLASSES_DIR" .
ok "JAR created: $DIST_DIR/$JAR_NAME"

# Copy dependency JARs alongside the main JAR
mkdir -p "$DIST_DIR/lib"
cp "$LIB_DIR"/*.jar "$DIST_DIR/lib/"

mkdir -p "$DIST_DIR/javafx"
cp "$JAVAFX_PATH"/*.jar "$DIST_DIR/javafx/"
ok "Dependencies copied to dist/"

# ---------------------------------------------------------------------------
# Step 6: Create native installer with jpackage
# ---------------------------------------------------------------------------
if ! command -v jpackage >/dev/null 2>&1; then
    err "jpackage not found -- skipping native installer."
    err "Install JDK 17+ with jpackage support to create installers."
    info "You can still run the app with:"
    info "  java --module-path \"$JAVAFX_PATH\" --add-modules $JAVAFX_MODULES -jar $DIST_DIR/$JAR_NAME"
    exit 0
fi

info "Creating native installer with jpackage..."

# Determine installer type based on OS
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
    Darwin*)
        PKG_TYPE="dmg"
        ;;
    *)
        PKG_TYPE="app-image"
        ;;
esac

info "  Installer type: $PKG_TYPE"

# Collect all dependency JARs into a single input directory for jpackage
JPACKAGE_INPUT="$BUILD_DIR/jpackage-input"
rm -rf "$JPACKAGE_INPUT"
mkdir -p "$JPACKAGE_INPUT/lib"

cp "$DIST_DIR/$JAR_NAME" "$JPACKAGE_INPUT/"
cp "$LIB_DIR"/*.jar "$JPACKAGE_INPUT/lib/"
cp "$JAVAFX_PATH"/*.jar "$JPACKAGE_INPUT/lib/"

# Icon option
JPACKAGE_ICON_OPT=""
if [ -f "$ICON_FILE" ]; then
    JPACKAGE_ICON_OPT="--icon $ICON_FILE"
    info "  Using icon: $ICON_FILE"
fi

# Linux-specific options (declare system dependencies for .deb)
LINUX_OPTS=""
if [ "$PKG_TYPE" = "deb" ]; then
    LINUX_OPTS="--linux-deb-maintainer suporte@naviera.com.br"
    LINUX_OPTS="$LINUX_OPTS --linux-app-category utilities"
    LINUX_OPTS="$LINUX_OPTS --linux-shortcut"
fi

# Build jpackage command with ALL required modules (JavaFX + Java platform)
jpackage \
    --input "$JPACKAGE_INPUT" \
    --main-jar "$JAR_NAME" \
    --main-class "$MAIN_CLASS" \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --vendor "$APP_VENDOR" \
    --description "$APP_DESCRIPTION" \
    --dest "$INSTALLER_DIR" \
    --type "$PKG_TYPE" \
    --module-path "$JAVAFX_PATH" \
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

ok "Native installer created in $INSTALLER_DIR/"

# ---------------------------------------------------------------------------
# Step 7: Patch .deb — inject JavaFX native libs into runtime
# ---------------------------------------------------------------------------
if [ "$PKG_TYPE" = "deb" ]; then
    DEB_FILE=$(ls "$INSTALLER_DIR"/*.deb 2>/dev/null | head -1)
    if [ -n "$DEB_FILE" ]; then
        info "Patching .deb with JavaFX native libraries..."
        PATCH_DIR=$(mktemp -d)
        dpkg-deb -x "$DEB_FILE" "$PATCH_DIR/root"
        dpkg-deb -e "$DEB_FILE" "$PATCH_DIR/DEBIAN"

        # Copy JavaFX native .so files into the runtime
        RUNTIME_LIB="$PATCH_DIR/root/opt/naviera/lib/runtime/lib"
        if [ -d "$RUNTIME_LIB" ]; then
            cp "$JAVAFX_PATH"/*.so "$RUNTIME_LIB/" 2>/dev/null || true
            SO_COUNT=$(ls "$RUNTIME_LIB"/libglass*.so 2>/dev/null | wc -l)
            ok "  Copied $SO_COUNT+ JavaFX native libs (.so)"
        fi

        # Verify java.sql is in the runtime
        JAVA_BIN="$PATCH_DIR/root/opt/naviera/lib/runtime/bin/java"
        if [ -x "$JAVA_BIN" ]; then
            if "$JAVA_BIN" --list-modules 2>/dev/null | grep -q "java.sql"; then
                ok "  java.sql module present in runtime"
            else
                err "  java.sql module MISSING — runtime may be incomplete"
            fi
        fi

        # Rebuild .deb (DEBIAN must be inside the root directory)
        mv "$PATCH_DIR/DEBIAN" "$PATCH_DIR/root/DEBIAN"
        dpkg-deb --build --root-owner-group "$PATCH_DIR/root" "$DEB_FILE"
        rm -rf "$PATCH_DIR"
        ok "  .deb patched successfully"
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
info ""
info "  To run directly:"
info "    java --module-path \"$JAVAFX_PATH\" \\"
info "         --add-modules $JAVAFX_MODULES \\"
info "         -cp \"$DIST_DIR/$JAR_NAME:$DIST_DIR/lib/*:$DIST_DIR/javafx/*\" \\"
info "         $MAIN_CLASS"
echo ""
