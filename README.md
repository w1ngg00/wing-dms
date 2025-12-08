# COMP2042 Coursework - Tetris Maintenance and Extension

---

## GitHub Repository
[https://github.com/w1ngg00/wing-dms](https://github.com/w1ngg00/wing-dms)

---

## Compilation Instructions

### Prerequisites
- **Java Development Kit (JDK)**: Version 11 or higher
- **JavaFX SDK**: Version 21 or compatible with your JDK
- **Maven**: Version 3.6 or higher (or use the included `mvnw`/`mvnw.cmd`)

### Step-by-Step Build Instructions

1. **Open the project** in IntelliJ IDEA or Visual Studio Code
   - Navigate to the project folder: `c:\Users\wingg\Downloads\COMP2042ThamWingLok\wing-dms-master\CW2025-master`

2. **Configure Maven**
   - Right-click on `pom.xml` in the project root
   - Select **"Add as Maven Project"** (or **"Reload All Maven Projects"** if already configured)
   - Wait for Maven to download dependencies

3. **Set Up Run Configuration**
   - Open **Run → Edit Configurations**
   - Create a new **Application** configuration
   - Set **Main class** to: `com.comp2042.view.Main`
   - Set **Working directory** to: `$PROJECT_DIR$`

4. **Configure JavaFX Module Options**
   - Click **Modify options** in the run configuration
   - Select **"Add VM options"**
   - Add the following to the **VM options** field:
