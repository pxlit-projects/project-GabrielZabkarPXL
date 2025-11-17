# Architecture

:heavy_check_mark: De API Gateway vormt het centrale toegangspunt tot het systeem en stuurt alle binnenkomende requests door naar de juiste microservice: PostService, ReviewService of CommentService. Alle services registreren zich bij Eureka zodat ze elkaar kunnen vinden, en hun configuratie wordt centraal opgehaald via de Config Server.

De microservices communiceren op twee manieren met elkaar. Voor directe, synchrone communicatie gebruik ik Spring Cloud OpenFeign: zowel de ReviewService als de CommentService doen via Feign aanroepen naar de PostService om postgegevens op te halen. Daarnaast is er ook asynchrone communicatie voorzien voor het reviewproces. Wanneer een post wordt ingediend voor review stuurt de PostService een event naar RabbitMQ. De ReviewService verwerkt dit event, en wanneer de reviewer de post goed- of afkeurt stuurt de ReviewService op zijn beurt weer een event terug. Dat event zorgt ervoor dat de PostService de status van de post kan aanpassen.

De CommentService gebruikt enkel synchrone communicatie. Die vraagt eerst via Feign aan de PostService of de post bestaat en gepubliceerd is, en slaat daarna de comments op of haalt ze op uit de eigen database.

<img width="1020" height="689" alt="image" src="https://github.com/user-attachments/assets/d072db8f-9380-4da5-92bd-cb3e400a013c" />
