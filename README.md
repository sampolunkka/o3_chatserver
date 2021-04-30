# o3_chatserver
Ohjelmointi 3 harjoitustyö
Sampo Lunkka
slunkka19@student.oulu.fi
2633569

Kommenteissa funktioiden kuvaukset

Mukana myö certiin liittyvät tiedostot:
- localhost.pem (clientiä varten)
- keystore.jks

Tietokanta:
- Tietokanta luodaan ensimmäisen ajon yhteydessä
- Jos paljon testailee, kannattaa poistaa aina välillä tai sqlitella dropata events
    -> koko kasvaa aika nopeesti palvelimen tapahtumilla
- Tähän oisi voinut vielä tehdä funktion, joka tallettaa taulun ja/tai tietokannan kun tietty koko saavutetaan tai palvelin suljetaan
