# Javelin HTTP Server 🚀

**Javelin** est un serveur HTTP léger écrit en Java, développé par **BRCloud**.  
Il permet d’héberger des fichiers statiques (HTML, CSS, JS) et propose des fonctionnalités avancées similaires à Apache/Nginx.

---

## ✨ Fonctionnalités
- 📂 Serveur de fichiers statiques (`www/`)
- ⚡ Logs Apache-like
- 📄 Pages d’erreur personnalisées (403, 404, 500)
- 📑 Directory listing
- 🔄 Compression Gzip
- 🔐 HTTPS (avec certificat auto-signé)
- 🌍 Virtual Hosts (multi-sites)
- 🛠️ Configuration externe (`server.conf`)
- 📊 Monitoring JSON via `/status`
- ⏱️ Exemple d’API REST via `/api/time`

---

## 🔧 Prérequis

Avant d’installer Javelin, assurez-vous d’avoir **Java (JDK)** installé.  
Sur **Debian/Ubuntu**, installez Java 17 (LTS) avec :

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk
```

Vérifiez ensuite la version :

```bash
java -version
```

Vous devriez voir quelque chose comme :

```
openjdk version "17.0.x" ...
```

---

## 🚀 Installation rapide (via GitHub)

Cloner directement le dépôt GitHub :

```bash
git clone https://github.com/Filox77250/Javelin.git
cd Javelin
javac Javelin.java
java Javelin
```

👉 Par défaut, le serveur démarre sur [http://localhost:8080](http://localhost:8080).

---

## 📦 Télécharger sans git (ZIP auto-généré par GitHub)

```bash
curl -L -o javelin.zip https://github.com/Filox77250/Javelin/archive/refs/heads/main.zip
unzip javelin.zip
cd Javelin-main
javac Javelin.java
java Javelin
```

---

## 📂 Structure du projet
```
Javelin/
├── Javelin.java        # Code source du serveur
├── server.conf         # Configuration (port, documentRoot, etc.)
├── www/                # Contenu statique
│   ├── index.html      # Page d’accueil (It works! style Apache2)
│   └── errors/         # Pages d’erreurs (403, 404, 500)
```

---

## 🔧 Exemple d’utilisation
- Placer vos fichiers dans `www/`
- Démarrer le serveur :  
  ```bash
  java Javelin
  ```
- Ouvrir [http://localhost:8080](http://localhost:8080)

---

## 👨‍💻 Auteur
Développé par **Filox77250** (BRCloud)  
👉 [https://github.com/Filox77250/Javelin](https://github.com/Filox77250/Javelin)
