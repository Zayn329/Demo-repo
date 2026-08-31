from sqlalchemy import Column, String, Integer, BigInteger, Boolean, JSON, Text
from app.db.database import Base

class DBUser(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, index=True)
    phone_number = Column(String, unique=True, index=True, nullable=False)
    created_at = Column(BigInteger, nullable=False)

class DBCircleMember(Base):
    __tablename__ = "circle_members"

    contact_id = Column(String, primary_key=True, index=True)
    display_name = Column(String, nullable=False)
    type = Column(String, nullable=False)
    phone_number = Column(String, nullable=True)
    app_user_id = Column(String, nullable=True)
    location_permission = Column(Boolean, default=True)
    notification_permission = Column(Boolean, default=True)

class DBSyncEvent(Base):
    __tablename__ = "sync_events"

    event_id = Column(String, primary_key=True, index=True)
    incident_id = Column(String, index=True, nullable=True)
    event_type = Column(String, nullable=False)
    occurred_at = Column(BigInteger, nullable=False)
    payload = Column(JSON, nullable=False)

class DBAnchor(Base):
    __tablename__ = "anchors"

    anchor_id = Column(String, primary_key=True, index=True)
    status = Column(String, nullable=False)
    transaction_hash = Column(String, nullable=False)
    created_at = Column(BigInteger, nullable=False)
