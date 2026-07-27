.PHONY: help test verify run infra-up infra-down package

help:
	@echo "make infra-up   Levanta PostgreSQL, Redis y Kafka"
	@echo "make run        Ejecuta la aplicación"
	@echo "make test       Ejecuta pruebas unitarias"
	@echo "make verify     Compila, prueba y genera cobertura"
	@echo "make package    Genera el JAR"

test:
	./mvnw test

verify:
	./mvnw clean verify

run:
	./mvnw spring-boot:run

infra-up:
	docker compose up -d postgres redis kafka

infra-down:
	docker compose down -v

package:
	./mvnw clean package
