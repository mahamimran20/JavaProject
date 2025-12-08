# 📘 CS4361 Group Project: Gradient Descent Visualization

Welcome to our group repository for the CS4361 course project. This space will host all code, documentation, and progress updates for our collaborative Java-based project.

## Running the project
Clone the repo with
```bash
git clone https://github.com/mahamimran20/JavaProject.git
```
and cd to the demo directory
```bash
cd ./JavaProject/demo
```
Next, the JavaFX SDK is needed, download the zip and put it into the demo directory. The latest version (25.0.1) is recommended. The example below uses the Linux x64 SDK. The link for other versions can be found at <https://gluonhq.com/products/javafx/>
```bash
curl -LO https://download2.gluonhq.com/openjfx/25.0.1/openjfx-25.0.1_linux-x64_bin-sdk.zip
unzip openjfx-25.0.1_linux-x64_bin-sdk.zip
```
Now the project can be compiled and run. Compile all .java files, and run App. The javafx.controls and javafx.graphics modules need to be added manually:
```bash
javac --module-path ./javafx-sdk-25.0.1/lib --add-modules javafx.controls,javafx.graphics *.java
java --module-path ./javafx-sdk-25.0.1/lib --add-modules javafx.controls,javafx.graphics App
```

## 👥 Team Memberss

- Maham — ⏳ Role to be assigned  
- Zain — ⏳ Role to be assigned  
- Luke — ⏳ Role to be assigned  
- Abdala — ⏳ Role to be assigned  
- Eyosias — ⏳ Role to be assigned

---

## 🧾 Deliverable 1: Project Phase I (5%) ✅

As a team, we will submit a **project proposal** (maximum 3 pages, single-spaced) aligned with our chosen theme. The proposal must include:

- **Project Title**
- **Course Name/Number**: CS4361
- **Goals of the Project**
- **Tools/Systems/Software** we plan to use  
  _Examples: Unity, C#, Java, OpenGL, Windows, Linux, GitHub, Eclipse, NetBeans_
- **Timeline of Development**
- **Individual Responsibilities** for each team member

### 📤 Submission Requirements
- **a.** Names of all contributors and their roles  
  _(Note: Non-contributors will receive a score of 0 for this part)_
- **b.** The project proposal document
- **c.** File name format: `CS4361_ProjectProposal_Team#`  
  _Example: `CS4361_ProjectProposal_Team10`_
- **d.** Submit only once per team via eLearning  
  **Due Date:** Wednesday, **10/08/25**

---

## 🧾 Project Progress ✅


## 📈 Deliverable 2: Project Progress Report (3%) ✅

This report will summarize our team’s progress and reflect on the development process. It should include:

- What has worked well so far
- Any improvements or pivots from the original proposal
- Communication tools/platforms used by the team
- Status of proposed deadlines
- Challenges faced and strategies to overcome them

### 📤 Submission Requirements
- **a.** Names of all contributors and their roles  
  _(Note: Non-contributors will receive a score of 0 for this part)_
- **b.** The project progress document
- **c.** File name format: `CS4361_ProjectProgress_Team#`  
  _Example: `CS4361_ProjectProgress_Team10`_
- **d.** Submit only once per team via eLearning  
  **Due Date:** Monday, **11/03/25**

---

## 👥 Collaboration Guidelines

- All team members will contribute via GitHub using branches and commits.
- Use clear commit messages and pull requests for code reviews.
- Track tasks and deadlines using GitHub Projects or Issues.

---

### 🔍 Emoji Legend

- ✅ — Task is **done** or **completed**
- ⏳ — Task is **pending**, needs to be **focused on** and **completed**
