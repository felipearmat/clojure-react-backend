CREATE TABLE operations (
  id SERIAL PRIMARY KEY,
  type VARCHAR(25) NOT NULL,
  cost float NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--;;
CREATE INDEX operations_type_ix on operations(type)
  WHERE deleted is not TRUE;
--;;
CREATE INDEX operations_id_ix on operations(id)
  WHERE deleted is not TRUE;
  --;;
CREATE INDEX operations_cost_ix on operations(cost)
  WHERE deleted is not TRUE;
--;;
CREATE INDEX operations_deleted_ix on operations(deleted);
--;;
CREATE UNIQUE INDEX operations_type_unique_constraint ON operations(type)
  WHERE deleted is not TRUE;
--;;
CREATE TRIGGER operations_auto_updated_at
  BEFORE UPDATE ON operations
  FOR EACH ROW
  EXECUTE PROCEDURE auto_updated_at();
