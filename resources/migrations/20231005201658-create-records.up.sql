CREATE TABLE records (
  id SERIAL PRIMARY KEY,
  operation_id INTEGER,
  user_id VARCHAR(36),
  amount float NOT NULL,
  user_balance float NOT NULL,
  operation_response VARCHAR(255),
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_operation
    FOREIGN KEY(operation_id)
	    REFERENCES operations(id),
  CONSTRAINT fk_user
    FOREIGN KEY(user_id)
	    REFERENCES users(id)
);
--;;
CREATE INDEX records_user_id_ix on records(user_id)
  WHERE deleted IS NOT TRUE;
--;;
CREATE INDEX records_operation_id_ix on records(operation_id)
  WHERE deleted IS NOT TRUE;
--;;
CREATE INDEX records_user_operation_id_ix on records(user_id, operation_id)
  WHERE deleted IS NOT TRUE;
--;;
CREATE INDEX records_created_at_ix on records(created_at)
  WHERE deleted IS NOT TRUE;
--;;
CREATE INDEX records_deleted_ix on records(deleted);
--;;
CREATE TRIGGER records_auto_updated_at
  BEFORE UPDATE ON records
  FOR EACH ROW
  EXECUTE PROCEDURE auto_updated_at();
