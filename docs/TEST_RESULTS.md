# Sam Shop - Ket qua test E2E

**Thá»i gian:** 2026-05-23 16:52:46  
**Gateway:** http://localhost:8088  
**Tá»•ng:** 34 | **PASS:** 34 | **FAIL:** 0

| ID | Module | Test case | Káº¿t quáº£ | Ghi chÃº |
|----|--------|-----------|---------|---------|| AUTH-01 | Auth | Login customer1 (USER) | **PASS** |  |
| AUTH-02 | Auth | Login staff1 (EMPLOYEE) | **PASS** |  |
| AUTH-03 | Auth | Login admin1 (ADMIN) | **PASS** |  |
| AUTH-04 | Auth | Reject invalid credentials | **PASS** |  |
| PRD-01 | Product | GET list products (public) | **PASS** |  |
| PRD-02 | Product | GET product by id (public) | **PASS** |  |
| PRD-03 | Product | GET products with search filter | **PASS** |  |
| PRD-04 | Product | POST create product (EMPLOYEE) | **PASS** |  |
| PRD-05 | Product | PUT update product (EMPLOYEE) | **PASS** |  |
| PRD-06 | Product | GET categories (public) | **PASS** |  |
| PRD-07 | Product | POST create category (EMPLOYEE) | **PASS** |  |
| PRD-08 | Product | PUT adjust stock (public) | **PASS** |  |
| PRD-09 | Product | DELETE product (ADMIN) | **PASS** |  |
| CRT-01 | Cart | POST add to cart (USER) | **PASS** |  |
| CRT-02 | Cart | GET cart by userId (USER) | **PASS** |  |
| CRT-03 | Cart | PUT update cart item (USER) | **PASS** |  |
| ORD-01 | Order | POST create order COD (USER) | **PASS** |  |
| ORD-02 | Order | GET my orders (USER) | **PASS** |  |
| ORD-03 | Order | GET order by id (USER) | **PASS** |  |
| PAY-01 | Payment | GET payment by orderId | **PASS** |  |
| ORD-04 | Order | PUT confirm order (EMPLOYEE) | **PASS** |  |
| ORD-05 | Order | PUT update status SHIPPING (EMPLOYEE) | **PASS** |  |
| ORD-06 | Order | PUT update status COMPLETED (EMPLOYEE) | **PASS** |  |
| PAY-02 | Payment | PUT confirm COD (EMPLOYEE) | **PASS** |  |
| ORD-07 | Order | GET all orders (EMPLOYEE) | **PASS** |  |
| ORD-08 | Order | GET revenue (ADMIN) | **PASS** |  |
| CRT-04 | Cart | POST add to cart for ONLINE flow | **PASS** |  |
| ORD-09 | Order | POST create order ONLINE (USER) | **PASS** |  |
| PAY-03 | Payment | POST initiate online payment | **PASS** |  |
| PAY-04 | Payment | POST online callback success | **PASS** |  |
| ORD-10 | Order | Verify ONLINE order paymentStatus PAID | **PASS** |  |
| CRT-05 | Cart | DELETE remove cart item | **PASS** |  |
| SEC-01 | Security | USER cannot access revenue (403) | **PASS** |  |
| SEC-02 | Security | Unauthenticated cart add (401) | **PASS** |  |

