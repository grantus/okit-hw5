# 1. Провести исправление проекта с использованием стат анализаторов по списку(Spotbugs, PMD)

## Reports до исправлений в коде

### PMD report

![Screenshot 2026-04-22 at 4.36.25 PM.png](pics/Screenshot%202026-04-22%20at%204.36.25%E2%80%AFPM.png)

### SpotBugs Report

![Screenshot 2026-04-22 at 4.35.41 PM.png](pics/Screenshot%202026-04-22%20at%204.35.41%E2%80%AFPM.png)
![Screenshot 2026-04-22 at 4.35.47 PM.png](pics/Screenshot%202026-04-22%20at%204.35.47%E2%80%AFPM.png)
![Screenshot 2026-04-22 at 4.35.53 PM.png](pics/Screenshot%202026-04-22%20at%204.35.53%E2%80%AFPM.png)
![Screenshot 2026-04-22 at 4.35.59 PM.png](pics/Screenshot%202026-04-22%20at%204.35.59%E2%80%AFPM.png)
![Screenshot 2026-04-22 at 4.36.04 PM.png](pics/Screenshot%202026-04-22%20at%204.36.04%E2%80%AFPM.png)

## Reports после исправлений

### PMD report

![Screenshot 2026-04-22 at 8.57.36 PM.png](pics/Screenshot%202026-04-22%20at%208.57.36%E2%80%AFPM.png)

### SpotBugs Report
![Screenshot 2026-04-22 at 8.58.00 PM.png](pics/Screenshot%202026-04-22%20at%208.58.00%E2%80%AFPM.png)
![Screenshot 2026-04-22 at 8.58.04 PM.png](pics/Screenshot%202026-04-22%20at%208.58.04%E2%80%AFPM.png)

# 2. Провести манипуляции с плагинами для OpenIDE

## Проанализировать сложность кода (Когнитивная сложность) — установить плагин из архива (code-complexity-plugin), найти метод с наибольшей сложностью в проекте, сделать скриншот, сохранить в отчет

Метод с самой большой когнитивной сложностью был найден в файле [OperationResponse.java](src/main/ru/hse/OperationResponse.java):

![Screenshot 2026-04-22 at 10.50.21 PM.png](pics/Screenshot%202026-04-22%20at%2010.50.21%E2%80%AFPM.png)

### Перевести проект на google стиль java кода

Скриншот файла [AccountManager.java](src/main/ru/hse/client/AccountManager.java) после перевода на google стиль:

![Screenshot 2026-04-22 at 11.47.56 PM.png](pics/Screenshot%202026-04-22%20at%2011.47.56%E2%80%AFPM.png)

