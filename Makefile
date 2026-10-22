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
## Runs as the host user with a throwaway DB so the CSV isn't root-owned.
refresh-snapshot: image
	@test -n "$(FRED_API_KEY)" || { echo "FRED_API_KEY is not set" >&2; exit 1; }
	docker run --rm -u $(shell id -u):$(shell id -g) -e ADP_DATA_DIR=/tmp -e FRED_API_KEY \
		-v $(CURDIR)/data:/out $(IMAGE) sync --source fred --export /out/adp_vintages.csv
