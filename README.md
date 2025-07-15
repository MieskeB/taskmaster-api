```bash
docker run -d \
  -p 8080:8080 \
  -v $(pwd)/uploads:/app/uploads \
  -e DB_URL=your-db-host \
  -e DB_PORT=3306 \
  -e DB_NAME=taskmaster \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=secret \
  -e UPLOAD_DIR=/app/uploads \
  -e ADMIN_CODE=supersecret \
  --name taskmaster-api \
  taskmaster-api-image
```

```yaml
version: "3.9"

services:
  taskmaster-api:
    build: .
    depends_on:
      - db
    ports:
      - "8080:8080"
    volumes:
      - ./uploads:/app/uploads
    environment:
      DB_URL: db
      DB_PORT: 3306
      DB_NAME: taskmaster
      DB_USERNAME: root
      DB_PASSWORD: secret
      ADMIN_CODE: supersecret

volumes:
  db_data:
```