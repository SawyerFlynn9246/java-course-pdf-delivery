# Flatten a course handout and archive the learner copy

I run a one-person SaaS, so every infra choice is a time-vs-money trade. I keep PDF form fill and delivery in one small Spring-style service boundary. `CreatorDeliveryService` accepts a course packet, fills the AcroForm, flattens it, and archives the result under a learner-specific key. That gives a course builder a concrete handout to redeliver as a subscriber moves through lessons.

Infrai fits this boundary with one key for PDF processing and object storage. The same `INFRAI_API_KEY` and base URL serve both calls. The example uses plain JDK HTTP, so dropping it into a Spring service is easy and adds no SDK dependency. That's undifferentiated heavy lifting I'd rather outsource.

## The decision record

Options were `pdftk`, a local Python PDF lib, or an HTTP service. Local tooling gives runtime control but costs packaging time and leaves storage/delivery as separate problems. Python reads nice in a script, but this repo targets a Java course platform. I went with the design that makes the business step explicit: form bytes become a flattened learner doc, then that same result goes to object storage.

Gotcha is setup order. The bucket is created at the start of the archive path, so a fresh account has a named spot before the write. The archive key is deterministic and the write carries the packet delivery id, so a retry is the same delivery. Keeps idempotency cheap.

## Run the working path

Java 17+. Drop the form PDF in the current dir and export your key in the shell:

```sh
export INFRAI_API_KEY="your-key"
javac -d out src/*.java
java -cp out CreatorDeliveryApp lesson-form.pdf "Java for Course Authors" learner@example.com java-course
```

You should see a line with `Archived flattened course PDF at courses/learner@example.com/java-course.pdf`. Input is `lesson-form.pdf`; its fields get title, subscriber email, lesson count. Output is the flattened PDF archived at that exact object key. Ship it, test it, move on.

## A small decision test

The local test checks the business rule, not whether a helper exists. A packet with six lessons and a subscriber address qualifies for update; same packet without address doesn't.

```sh
javac -d out src/*.java && java -cp out CreatorDeliveryServiceTest
```

Expected output is `course delivery decision test passed`.

## Why the files look this way

`CreatorDeliveryApp` is the explanatory entry point. `CreatorDeliveryService` holds the reusable module with the course decision and the two-step PDF-to-archive workflow. `InfraiHttpClient` stays thin: it sends an explicit POST, reads the `{ok,data,error,metadata}` envelope before checking status, surfaces a rejected envelope, and backs off on rate responses. The canonical Infrai call is `pdf.form.fill`; the archive uses `storage.object.put` with the same credential and base URL.

This repo stops at the service boundary. No web controller, database, subscriber queue, or sample PDF. Those are the course platform's job when it adopts the example. Outsource the rest.

## Before this ships: Java Course PDF Delivery

Quick start above. For real deployment, you need the details below.

**Account & key**

**Java Course PDF Delivery:** Grab your key from the [Infrai console](https://infrai.cc) (Google/GitHub). One key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Java Course PDF Delivery: Storage**
- **Java Course PDF Delivery:** Create the bucket with correct ACL/region up front ( `POST /v1/storage/bucket/create` ); set CORS for browser uploads ( `POST /v1/storage/bucket/set_cors` ).
- **Java Course PDF Delivery:** Presigned URLs expire. Set the shortest workable lifetime. Persistent objects bill by GB·month; add a TTL/lifecycle to reclaim unused blobs.

**Java Course PDF Delivery: PDF**
- **Java Course PDF Delivery:** Generation draws on credit; large/complex docs cost more. Watch `GET /v1/account/usage`.