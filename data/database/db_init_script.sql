CREATE TABLE Users (
  id                INT           NOT NULL AUTO_INCREMENT,
  Email             VARCHAR(255)  NOT NULL UNIQUE,
  Passwordhash      TEXT          NOT NULL,
  Salt              TEXT          NOT NULL,
  AdminFlag         BOOL          NOT NULL DEFAULT 0,
  Status            ENUM ('needs_verification','verified','deleted')
                                  NOT NULL DEFAULT 'needs_verification',
  FirstName         TEXT          NOT NULL,
  LastName          TEXT          NOT NULL,
  Address           TEXT          NOT NULL,
  RegistrationDate  DATETIME      NOT NULL,
  VerifiedDate      DATETIME               DEFAULT NULL,
  PRIMARY KEY (ID)
);

CREATE TABLE Requests (
  id              INT           NOT NULL AUTO_INCREMENT,
  UserID          INT           NOT NULL REFERENCES Users (id),
  Algorithm       VARCHAR(255)  NOT NULL,
  JSONRequest     LONGBLOB,
  JSONResponse    LONGBLOB               DEFAULT NULL,
  Cost            INT           NOT NULL DEFAULT 0,
  RequestDate     DATETIME      NOT NULL,
  FinishedDate    DATETIME               DEFAULT NULL,
  CPUTime         BIGINT        NOT NULL DEFAULT 0,
  Status          ENUM ('ok','pending','failed')
                                NOT NULL DEFAULT 'pending',
  PRIMARY KEY (ID),
  FOREIGN KEY (UserID) REFERENCES Users (id)
);

