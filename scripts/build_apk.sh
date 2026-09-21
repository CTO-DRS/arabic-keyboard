#!/bin/bash
# =====================================================================
# بناء حزمة APK للوحة المفاتيح العربية (بدون Gradle)
# aapt2 + javac + d8(R8) + zipalign + apksigner
#
# يعمل محلياً وعلى CI. المتغيرات البيئية الاختيارية:
#   ANDROID_SDK_ROOT   مسار Android SDK (الافتراضي: /home/z/my-project/android-sdk)
#   ANDROID_BUILD_TOOLS نسخة build-tools (الافتراضي: 34.0.0)
#   JAVA_HOME          مسار JDK (إن لم يكن java على PATH)
#   KB_STORE_PASS      كلمة سر مفتاح التوقيع (الافتراضي: kb-ar-2024)
# =====================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJ="$(dirname "$SCRIPT_DIR")"

# اكتشاف مسار Android SDK تلقائياً
BT_VERSION="${ANDROID_BUILD_TOOLS:-34.0.0}"
SDK="${ANDROID_SDK_ROOT:-}"
if [ -z "$SDK" ]; then
    for CAND in "$HOME/Android/Sdk" \
                "/usr/local/lib/android/sdk" "/opt/android-sdk" \
                "/home/z/my-project/android-sdk"; do
        if [ -n "$CAND" ] && [ -d "$CAND/build-tools" ]; then
            SDK="$CAND"
            break
        fi
    done
fi
[ -z "$SDK" ] && { echo "خطأ: لم يتم العثور على Android SDK — اضبط ANDROID_SDK_ROOT"; exit 1; }
BT="$SDK/build-tools/$BT_VERSION"
PLAT="$SDK/platforms/android-34/android.jar"

[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"
JAVA_BIN="$(command -v java || true)"
[ -z "$JAVA_BIN" ] && { echo "خطأ: java غير متوفر. اضبط JAVA_HOME"; exit 1; }
JAVAC_BIN="$(command -v javac || true)"
[ -z "$JAVAC_BIN" ] && { echo "خطأ: javac غير متوفر (ثبّت JDK كاملاً)"; exit 1; }

OUT="$PROJ/build"
DIST="${KB_DIST_DIR:-$PROJ/dist}"
KS="$PROJ/keystore.jks"
KS_ALIAS="kb"
KS_PASS="pass:${KB_STORE_PASS:-kb-ar-2024}"
FINAL_APK="$DIST/Arabic-Keyboard.apk"

# أداة R8 المستقلة (d8 القديم في build-tools 34 لا يدعم مخرجات JDK 21)
R8_JAR="${R8_JAR:-$PROJ/.tools/r8.jar}"
if [ ! -f "$R8_JAR" ]; then
    echo "==> تنزيل R8 8.5.35"
    mkdir -p "$(dirname "$R8_JAR")"
    curl -sL -o "$R8_JAR" \
        "https://dl.google.com/android/maven2/com/android/tools/r8/8.5.35/r8-8.5.35.jar"
fi

echo "==> [1/7] تجهيز المجلدات"
rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$DIST"

echo "==> [2/7] تجميع الموارد (aapt2 compile)"
"$BT/aapt2" compile --dir "$PROJ/res" -o "$OUT/res.zip"

echo "==> [3/7] ربط الموارد وإنشاء R.java (aapt2 link)"
"$BT/aapt2" link -o "$OUT/base.apk" \
    -I "$PLAT" \
    --manifest "$PROJ/AndroidManifest.xml" \
    --min-sdk-version 21 --target-sdk-version 34 \
    --java "$OUT/gen" \
    --auto-add-overlay \
    "$OUT/res.zip"

echo "==> [4/7] ترجمة كود Java (javac)"
find "$OUT/gen" "$PROJ/src" -name "*.java" > "$OUT/sources.txt"
javac -source 8 -target 8 -encoding UTF-8 \
    -bootclasspath "$PLAT:$BT/core-lambda-stubs.jar" \
    -classpath "$PLAT" \
    -d "$OUT/classes" \
    @"$OUT/sources.txt" 2> "$OUT/javac.log" || { cat "$OUT/javac.log"; exit 1; }
echo "    تمت الترجمة بنجاح"

echo "==> [5/7] تحويل bytecode إلى DEX (d8/R8 8.5)"
(cd "$OUT/classes" && jar cf "$OUT/classes.jar" .)
java -cp "$R8_JAR" com.android.tools.r8.D8 \
    --release --lib "$PLAT" --min-api 21 \
    --output "$OUT/dex" "$OUT/classes.jar"

echo "==> [6/7] حزم DEX داخل APK ومحاذاة (zipalign)"
python3 - "$OUT/base.apk" "$OUT/dex/classes.dex" <<'PYEOF'
import sys, zipfile
apk, dex = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(apk, 'a') as z:
    z.write(dex, 'classes.dex', zipfile.ZIP_DEFLATED)
print("    classes.dex أُضيف إلى الحزمة")
PYEOF
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"

echo "==> [7/7] التوقيع الرقمي (apksigner)"
if [ ! -f "$KS" ]; then
    keytool -genkeypair -keystore "$KS" -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 \
        -validity 10000 -storepass "${KB_STORE_PASS:-kb-ar-2024}" \
        -keypass "${KB_STORE_PASS:-kb-ar-2024}" \
        -dname "CN=Arabic Keyboard, OU=Apps, O=Zai, C=SA"
    echo "    تم إنشاء مفتاح توقيع جديد (احتفظ به للتحديثات)"
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass "$KS_PASS" --key-pass "$KS_PASS" \
    --out "$FINAL_APK" "$OUT/aligned.apk"

echo "==> التحقق من الحزمة"
"$BT/apksigner" verify "$FINAL_APK" && echo "    التوقيع سليم ✔"
"$BT/aapt" dump badging "$FINAL_APK" | grep -E "^package|application-label:" | head -3
echo ""
ls -lh "$FINAL_APK"
echo "اكتمل البناء: $FINAL_APK"
