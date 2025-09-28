#!/bin/bash
# Script d'installation de Javelin en tant que service systemd 🚀

SERVICE_NAME="javelin"
INSTALL_DIR="/opt/Javelin"

echo "📦 Compilation de Javelin..."
javac -encoding UTF-8 Javelin.java
if [ $? -ne 0 ]; then
  echo "❌ Erreur lors de la compilation"
  exit 1
fi

echo "📂 Création de l'archive JAR..."
jar --create --file Javelin.jar --main-class=Javelin Javelin.class

echo "📂 Création du répertoire d'installation : $INSTALL_DIR"
sudo mkdir -p $INSTALL_DIR
sudo cp -r Javelin.java Javelin.class Javelin.jar server.conf www README.md $INSTALL_DIR/ 2>/dev/null

echo "⚙️ Création du service systemd : /etc/systemd/system/$SERVICE_NAME.service"
sudo bash -c "cat > /etc/systemd/system/$SERVICE_NAME.service <<EOL
[Unit]
Description=Javelin HTTP Server
After=network.target

[Service]
User=root
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/java -jar $INSTALL_DIR/Javelin.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
EOL"

echo "🔄 Rechargement de systemd..."
sudo systemctl daemon-reload

echo "✅ Activation du service Javelin au démarrage"
sudo systemctl enable $SERVICE_NAME

echo "🚀 Démarrage du service Javelin"
sudo systemctl start $SERVICE_NAME

echo "📊 Vérification du statut :"
sudo systemctl status $SERVICE_NAME --no-pager -l
