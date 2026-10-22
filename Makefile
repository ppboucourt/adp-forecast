IMAGE       := adp-forecast
BUILD_IMAGE := adp-forecast-build
VOLUME      := adp-forecast-data
GRADLE      := ./gradlew --no-daemon

.PHONY: init image build-image run test lint format refresh-snapshot

## Build the runtime image and create the database volume.
init: image
	docker volume create $(VOLUME) > /dev/null

image:
	docker build -t $(IMAGE) .

build-image:
	docker build --target build -t $(BUILD_IMAGE) .

## Run a subcommand: make run ARGS="forecast --explain"
run:
	docker run --rm -v $(VOLUME):/data -e FRED_API_KEY $(IMAGE) $(ARGS)

test: build-image
	docker run --rm $(BUILD_IMAGE) $(GRADLE) test

lint: build-image
	docker run --rm $(BUILD_IMAGE) $(GRADLE) spotlessCheck

## Rewrite sources in place with google-java-format.
format: build-image
	docker run --rm -v $(CURDIR)/src:/workspace/src $(BUILD_IMAGE) \
		sh -c '$(GRADLE) spotlessApply && chown -R $(shell id -u):$(shell id -g) src'

## Maintainer only: pull every vintage from FRED and rewrite data/adp_vintages.csv.
refresh-snapshot:
	@test -n "$(FRED_API_KEY)" || { echo "FRED_API_KEY is not set" >&2; exit 1; }
	@echo "refresh-snapshot: FredSource is not implemented yet" >&2; exit 1
