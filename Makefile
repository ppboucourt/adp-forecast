IMAGE       := adp-forecast
BUILD_IMAGE := adp-forecast-build
VOLUME      := adp-forecast-data
GRADLE      := ./gradlew --no-daemon

# Local secrets (gitignored): a .env file with the line FRED_API_KEY=<key> (no quotes, no spaces).
-include .env
export FRED_API_KEY

.PHONY: init image build-image run test lint format refresh-snapshot

## Build the image and seed the database from the committed snapshot; also sync from FRED when
## FRED_API_KEY is set. Re-running is safe: stored vintages are never duplicated.
init: image
	docker volume create $(VOLUME) > /dev/null
	$(MAKE) --no-print-directory run ARGS="sync --source csv"
	@if [ -n "$(FRED_API_KEY)" ]; then $(MAKE) --no-print-directory run ARGS="sync --source fred"; fi

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
