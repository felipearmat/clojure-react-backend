CREATE TABLE credits (
  id VARCHAR(36) PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id VARCHAR(36),
  value float NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_user
    FOREIGN KEY(user_id)
	    REFERENCES users(id)
);
--;;
CREATE INDEX credits_user_id_ix on credits(user_id)
  WHERE deleted IS NOT TRUE;
--;;
CREATE INDEX credits_created_at_ix on credits(created_at)
  WHERE deleted IS NOT TRUE;
--;;
CREATE INDEX credits_deleted_ix on credits(deleted);
--;;
CREATE TRIGGER credits_auto_updated_at
  BEFORE UPDATE ON credits
  FOR EACH ROW
  EXECUTE PROCEDURE auto_updated_at();
