# 🦊 Ladefuchs - Android

## 🦭 Developer manifesto

- We code for people, not machines. Keep your comments, PR remarks friendly and encourage constructive work. Also respect those not identifying with one of the binary genders by using the pronouns they provide.
- Developers will have different skills. Some changes might have been made in a rush, or maybe in a stressed environment. This should not be used as an excuse for bad code *however* we are all humans - treat every contributor with respect.
- Write commit-messages by finishing the sentence: "If applied, this commit will…" to enable cherry-picking.

## 🙋🏻‍♂️ How to contribute

1. Take one of the issues on this repository and assign it to yourself.
2. If necessary, ask Malik, Basti, for additional information.
3. Create a feature/bugfix branch for your implementation.
4. Create a pull-request.

## 🤓 Contents of a pull-request

1. A detailed description what you changed, and why.
2. All necessary files and information to run the project.
3. If there are any visual changes, upload screenshots and / or videos.
4. Link the issue the pull-request originates from.

## 👩🏼‍💼 Licensing

This project is FLOSS under [Apache License 2.0](https://choosealicense.com/licenses/apache-2.0/#), some assets in the repository (e.g. especially the charging-card logos/graphics) are under 3rd party copyrights of their companies/individuals.

Happy coding!

🖖🏻

## Getting Started

* Clone the repo to your local machine: `git clone git@github.com:Team-Ladefuchs/ladefuchs-android.git`
  * (create `Ladefuchs/secrets.properties`)
* Download and install [Android Studio](https://developer.android.com/studio)
* Start it, on the "Welcome to Android Studio" screen, click "Open" on top right, navigate to the location you cloned the repository to, and select the `Ladefuchs/` (not `Ladefuchs/app/` or `Ladefuchs/ladefuchs/`) subfolder (will take a while for the build to be set up and commence)
* Connect a USB debugging enabled device
* Click "Run `app`" on top (will take a while)
* Should automatically show on the phone

## Available at
* https://play.google.com/store/apps/details?id=app.ladefuchs.android
* https://f-droid.org/de/packages/app.ladefuchs.android/ - Metadata at https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/app.ladefuchs.android.yml

## Was macht diese App?

Was kostet der Strom für Dein E-Auto??
Blitzschnell die günstigste Ladekarte finden.

Mit einem Fingerwischen (Auswahl des Ladesäulen-Betreibers) zeigt Dir der Ladefuchs die günstigste Ladekarte an der EV-Ladesäule.

Nicht mehr, nicht weniger.

Dank [chargeprice.app](https://www.chargeprice.app/) sind die Daten immer aktuell.

Die App ist kostenlos. Der kleine Ladefuchs freut sich allerdings wie ein junger Dachs über Futterspenden.

## Wer ist eigentlich schuld?

* Malik & Flowinho von audiodump.de
* Bastian ’Schlingel‘ Wölfle von bitsundso.de
* Dominic Wrege
* Thorsten Rösler
* Illu: Aga und Marcel-André

## Ideen? Bugs?

Bitte hier eintragen https://github.com/Team-Ladefuchs/ladefuchs-android/issues/new oder an android@ladefuchs.app schicken.

Falls du Vorschläge für Fußnotentexte hast, lass es uns wissen auf https://ladefuchs.app/fussnote

## Anti-Features/Unerwünschte Merkmale

* Non-Free Network Services / Proprietäre Netzwerkdienste - Die App greift auf einen eigenen API-Server zu, der wiederum Daten von chargeprice.app bezieht.

## Permissions / Berechtigungen

* INTERNET - Zugriff auf den eigenen API-Server
* ACCESS_NETWORK_STATE - Überprüfen, ob eine Internetverbindung besteht, bevor versucht wird auf, den API-Server zuzugreifen
