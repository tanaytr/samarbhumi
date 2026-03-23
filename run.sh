#!/bin/bash
echo "================================================"
echo " SAMARBHUMI ANTIM - War Never Ends"
echo " Java 2D Action Shooter"
echo "================================================"
echo ""

mkdir -p bin saves

echo "[1/3] Compiling..."
javac -encoding UTF-8 -d bin \
  src/com/samarbhumi/exception/*.java \
  src/com/samarbhumi/core/Vec2.java \
  src/com/samarbhumi/core/AABB.java \
  src/com/samarbhumi/core/GameConstants.java \
  src/com/samarbhumi/core/Enums.java \
  src/com/samarbhumi/core/InputState.java \
  src/com/samarbhumi/physics/PhysicsBody.java \
  src/com/samarbhumi/map/GameMap.java \
  src/com/samarbhumi/map/MapFactory.java \
  src/com/samarbhumi/weapon/Weapon.java \
  src/com/samarbhumi/weapon/Projectile.java \
  src/com/samarbhumi/weapon/ParticleSystem.java \
  src/com/samarbhumi/entity/Player.java \
  src/com/samarbhumi/entity/PickupItem.java \
  src/com/samarbhumi/ai/BotController.java \
  src/com/samarbhumi/progression/PlayerProfile.java \
  src/com/samarbhumi/core/GameSession.java \
  src/com/samarbhumi/audio/AudioEngine.java \
  src/com/samarbhumi/ui/PlayerRenderer.java \
  src/com/samarbhumi/ui/MapRenderer.java \
  src/com/samarbhumi/ui/HUDRenderer.java \
  src/com/samarbhumi/ui/UIRenderer.java \
  src/com/samarbhumi/ui/GameScreen.java \
  src/com/samarbhumi/ui/Screens.java \
  src/com/samarbhumi/ui/GameWindow.java \
  src/Main.java

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Compilation failed. Ensure JDK 17+ is installed."
    echo "  Download: https://adoptium.net"
    exit 1
fi

echo "[2/3] Copying resources..."
[ -f resources/fonts/GameFont-Bold.ttf ]    && cp resources/fonts/GameFont-Bold.ttf    bin/
[ -f resources/fonts/GameFont-Regular.ttf ] && cp resources/fonts/GameFont-Regular.ttf bin/

echo "[3/3] Launching Samarbhumi Antim..."
echo ""
java -Xmx512m -Xms128m -Dsun.java2d.opengl=true -Dfile.encoding=UTF-8 -cp bin Main
