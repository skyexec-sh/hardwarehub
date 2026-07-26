package com.hardwarehub.common.config;

import com.hardwarehub.catalog.domain.Product;
import com.hardwarehub.catalog.repository.ProductRepository;
import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.repository.CustomerRepository;
import com.hardwarehub.fulfillment.domain.DeliveryReceipt;
import com.hardwarehub.fulfillment.domain.DeliveryReceiptItem;
import com.hardwarehub.fulfillment.domain.DeliveryReceiptStatus;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoice;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceItem;
import com.hardwarehub.fulfillment.domain.FulfillmentInvoiceStatus;
import com.hardwarehub.fulfillment.domain.Quotation;
import com.hardwarehub.fulfillment.domain.QuotationItem;
import com.hardwarehub.fulfillment.domain.QuotationStatus;
import com.hardwarehub.fulfillment.domain.SalesOrder;
import com.hardwarehub.fulfillment.domain.SalesOrderItem;
import com.hardwarehub.fulfillment.domain.SalesOrderStatus;
import com.hardwarehub.fulfillment.repository.DeliveryReceiptRepository;
import com.hardwarehub.fulfillment.repository.FulfillmentInvoiceRepository;
import com.hardwarehub.fulfillment.repository.QuotationRepository;
import com.hardwarehub.fulfillment.repository.SalesOrderRepository;
import com.hardwarehub.inventory.domain.InventoryTransactionType;
import com.hardwarehub.inventory.dto.InventoryTransactionRequest;
import com.hardwarehub.inventory.service.InventoryService;
import com.hardwarehub.pricing.service.PricingService;
import com.hardwarehub.sales.domain.PaymentMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds Milestone 8 sample documents so Quotes / Orders / Invoices can be explored end-to-end.
 * Idempotent: skipped when {@code QUO-DEMO-DRAFT} already exists.
 */
@Component
@Order(200)
@RequiredArgsConstructor
@Slf4j
public class FulfillmentDemoSeeder implements ApplicationRunner {

    private final QuotationRepository quotationRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final DeliveryReceiptRepository deliveryReceiptRepository;
    private final FulfillmentInvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    private final InventoryService inventoryService;
    private final AuditService auditService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (quotationRepository.findByQuoteNumber("QUO-DEMO-DRAFT").isPresent()) {
            return;
        }

        Customer retail = requireCustomer("DEMO-RETAIL");
        Customer contractor = requireCustomer("DEMO-CONTRACTOR");
        Customer vip = requireCustomer("DEMO-VIP");

        List<Product> products = productRepository
                .search("", null, null, "", "", true, false, PageRequest.of(0, 4))
                .getContent();
        if (products.size() < 2) {
            log.warn("Skipping M8 demo seed: need at least 2 active products");
            return;
        }

        Product p1 = products.get(0);
        Product p2 = products.get(1);

        seedDraftQuote(retail, p1, p2);
        seedSentQuote(contractor, p1);
        seedAcceptedQuote(vip, p1, p2);
        seedOpenOrder(contractor, p1, p2);
        seedPartialOrder(vip, p1);
        seedPaidCashOrder(retail, p2);
        seedOpenCreditInvoice(contractor, p1);

        auditService.log("SEED", "FULFILLMENT", "demo", "M8 sample quotes/orders/invoices");
        log.info("Seeded Milestone 8 fulfillment demo documents");
    }

    private void seedDraftQuote(Customer customer, Product a, Product b) {
        Quotation q = newQuote("QUO-DEMO-DRAFT", customer, QuotationStatus.DRAFT, "DEMO-M8 draft — edit or send");
        q.setValidUntil(LocalDate.now().plusDays(14));
        addQuoteLine(q, a, customer, bd("10"), null);
        addQuoteLine(q, b, customer, bd("5"), null);
        finalizeQuoteTotals(q);
        quotationRepository.save(q);
    }

    private void seedSentQuote(Customer customer, Product product) {
        Quotation q = newQuote("QUO-DEMO-SENT", customer, QuotationStatus.SENT, "DEMO-M8 sent — accept or reject");
        q.setValidUntil(LocalDate.now().plusDays(7));
        addQuoteLine(q, product, customer, bd("20"), null);
        finalizeQuoteTotals(q);
        quotationRepository.save(q);
    }

    private void seedAcceptedQuote(Customer customer, Product a, Product b) {
        Quotation q = newQuote("QUO-DEMO-ACCEPT", customer, QuotationStatus.ACCEPTED, "DEMO-M8 accepted — convert to SO");
        q.setValidUntil(LocalDate.now().plusDays(21));
        addQuoteLine(q, a, customer, bd("50"), null);
        addQuoteLine(q, b, customer, bd("12"), null);
        finalizeQuoteTotals(q);
        quotationRepository.save(q);
    }

    private void seedOpenOrder(Customer customer, Product a, Product b) {
        Quotation q = newQuote("QUO-DEMO-OPEN", customer, QuotationStatus.CONVERTED, "DEMO-M8 converted to open SO");
        addQuoteLine(q, a, customer, bd("30"), null);
        addQuoteLine(q, b, customer, bd("8"), null);
        finalizeQuoteTotals(q);
        quotationRepository.save(q);

        SalesOrder order = newOrder("SO-DEMO-OPEN", customer, q, SalesOrderStatus.OPEN, "DEMO-M8 open — deliver anytime");
        copyQuoteToOrder(q, order);
        salesOrderRepository.save(order);
    }

    private void seedPartialOrder(Customer customer, Product product) {
        Quotation q = newQuote("QUO-DEMO-PARTIAL", customer, QuotationStatus.CONVERTED, "DEMO-M8 partial delivery scenario");
        addQuoteLine(q, product, customer, bd("100"), null);
        finalizeQuoteTotals(q);
        quotationRepository.save(q);

        SalesOrder order = newOrder(
                "SO-DEMO-PARTIAL", customer, q, SalesOrderStatus.PARTIALLY_DELIVERED, "DEMO-M8 ordered 100 / delivered 65");
        copyQuoteToOrder(q, order);
        SalesOrderItem line = order.getItems().get(0);
        BigDecimal delivered = bd("65");
        line.setQuantityDelivered(delivered);
        salesOrderRepository.save(order);

        DeliveryReceipt dr = new DeliveryReceipt();
        dr.setDrNumber("DR-DEMO-PARTIAL");
        dr.setSalesOrder(order);
        dr.setStatus(DeliveryReceiptStatus.POSTED);
        dr.setNotes("DEMO-M8 first truck — 65 of 100");
        dr.setCreatedBy("system");

        DeliveryReceiptItem dri = new DeliveryReceiptItem();
        dri.setSalesOrderItem(line);
        dri.setLineNo(1);
        dri.setProduct(product);
        dri.setProductSku(line.getProductSku());
        dri.setProductName(line.getProductName());
        dri.setUnit(line.getUnit());
        dri.setQuantity(delivered);
        dri.setUnitPrice(line.getUnitPrice());
        dr.addItem(dri);
        deliveryReceiptRepository.save(dr);

        stockOutSafe(product, delivered, dr.getDrNumber(), order.getSoNumber());
    }

    private void seedPaidCashOrder(Customer customer, Product product) {
        Quotation q = newQuote("QUO-DEMO-CASH", customer, QuotationStatus.CONVERTED, "DEMO-M8 fully delivered + cash invoice");
        addQuoteLine(q, product, customer, bd("15"), null);
        finalizeQuoteTotals(q);
        quotationRepository.save(q);

        SalesOrder order = newOrder(
                "SO-DEMO-CASH", customer, q, SalesOrderStatus.FULLY_DELIVERED, "DEMO-M8 fully delivered (cash)");
        copyQuoteToOrder(q, order);
        SalesOrderItem line = order.getItems().get(0);
        line.setQuantityDelivered(line.getQuantityOrdered());
        line.setQuantityInvoiced(line.getQuantityOrdered());
        salesOrderRepository.save(order);

        DeliveryReceipt dr = new DeliveryReceipt();
        dr.setDrNumber("DR-DEMO-CASH");
        dr.setSalesOrder(order);
        dr.setStatus(DeliveryReceiptStatus.POSTED);
        dr.setNotes("DEMO-M8 complete delivery");
        dr.setCreatedBy("system");
        DeliveryReceiptItem dri = new DeliveryReceiptItem();
        dri.setSalesOrderItem(line);
        dri.setLineNo(1);
        dri.setProduct(product);
        dri.setProductSku(line.getProductSku());
        dri.setProductName(line.getProductName());
        dri.setUnit(line.getUnit());
        dri.setQuantity(line.getQuantityOrdered());
        dri.setUnitPrice(line.getUnitPrice());
        dr.addItem(dri);
        deliveryReceiptRepository.save(dr);
        stockOutSafe(product, line.getQuantityOrdered(), dr.getDrNumber(), order.getSoNumber());

        FulfillmentInvoice inv = newInvoice(
                "INV-DEMO-CASH", order, customer, PaymentMethod.CASH, FulfillmentInvoiceStatus.PAID, "DEMO-M8 paid cash invoice");
        inv.setAmountPaid(order.getTotalAmount());
        inv.setSubtotal(order.getSubtotal());
        inv.setDiscountAmount(order.getDiscountAmount());
        inv.setTaxAmount(order.getTaxAmount());
        inv.setTotalAmount(order.getTotalAmount());
        addInvoiceLine(inv, line, line.getQuantityOrdered());
        invoiceRepository.save(inv);
    }

    private void seedOpenCreditInvoice(Customer customer, Product product) {
        Quotation q = newQuote("QUO-DEMO-CREDIT", customer, QuotationStatus.CONVERTED, "DEMO-M8 credit invoice awaiting payment");
        addQuoteLine(q, product, customer, bd("25"), null);
        finalizeQuoteTotals(q);
        quotationRepository.save(q);

        SalesOrder order = newOrder(
                "SO-DEMO-CREDIT", customer, q, SalesOrderStatus.FULLY_DELIVERED, "DEMO-M8 credit — record payment");
        copyQuoteToOrder(q, order);
        SalesOrderItem line = order.getItems().get(0);
        line.setQuantityDelivered(line.getQuantityOrdered());
        line.setQuantityInvoiced(line.getQuantityOrdered());
        salesOrderRepository.save(order);

        DeliveryReceipt dr = new DeliveryReceipt();
        dr.setDrNumber("DR-DEMO-CREDIT");
        dr.setSalesOrder(order);
        dr.setStatus(DeliveryReceiptStatus.POSTED);
        dr.setNotes("DEMO-M8 credit delivery");
        dr.setCreatedBy("system");
        DeliveryReceiptItem dri = new DeliveryReceiptItem();
        dri.setSalesOrderItem(line);
        dri.setLineNo(1);
        dri.setProduct(product);
        dri.setProductSku(line.getProductSku());
        dri.setProductName(line.getProductName());
        dri.setUnit(line.getUnit());
        dri.setQuantity(line.getQuantityOrdered());
        dri.setUnitPrice(line.getUnitPrice());
        dr.addItem(dri);
        deliveryReceiptRepository.save(dr);
        stockOutSafe(product, line.getQuantityOrdered(), dr.getDrNumber(), order.getSoNumber());

        FulfillmentInvoice inv = newInvoice(
                "INV-DEMO-CREDIT",
                order,
                customer,
                PaymentMethod.CREDIT,
                FulfillmentInvoiceStatus.OPEN,
                "DEMO-M8 open credit — use Record payment");
        inv.setAmountPaid(BigDecimal.ZERO);
        inv.setSubtotal(order.getSubtotal());
        inv.setDiscountAmount(order.getDiscountAmount());
        inv.setTaxAmount(order.getTaxAmount());
        inv.setTotalAmount(order.getTotalAmount());
        addInvoiceLine(inv, line, line.getQuantityOrdered());
        invoiceRepository.save(inv);

        BigDecimal outstanding = customer.getOutstandingBalance() == null ? BigDecimal.ZERO : customer.getOutstandingBalance();
        customer.setOutstandingBalance(outstanding.add(order.getTotalAmount()));
        customer.setUpdatedBy("system");
        customerRepository.save(customer);
    }

    private void stockOutSafe(Product product, BigDecimal qty, String drNumber, String soNumber) {
        Product locked = productRepository
                .findByIdForUpdate(product.getId())
                .orElseThrow(() -> new IllegalStateException("Product missing: " + product.getId()));
        BigDecimal stock = locked.getCurrentStock() == null ? BigDecimal.ZERO : locked.getCurrentStock();
        if (stock.compareTo(qty) < 0) {
            locked.setCurrentStock(qty);
            locked.setUpdatedBy("system");
            productRepository.saveAndFlush(locked);
        }
        inventoryService.create(new InventoryTransactionRequest(
                product.getId(),
                InventoryTransactionType.STOCK_OUT,
                qty,
                product.getCostPrice(),
                drNumber,
                "Demo delivery " + drNumber + " / " + soNumber));
    }

    private Quotation newQuote(String number, Customer customer, QuotationStatus status, String notes) {
        Quotation q = new Quotation();
        q.setQuoteNumber(number);
        q.setCustomer(customer);
        q.setStatus(status);
        q.setNotes(notes);
        q.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        q.setTaxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        q.setCreatedBy("system");
        q.setUpdatedBy("system");
        return q;
    }

    private void addQuoteLine(Quotation q, Product product, Customer customer, BigDecimal qty, BigDecimal unitPriceOverride) {
        BigDecimal unitPrice = unitPriceOverride != null
                ? money(unitPriceOverride)
                : money(pricingService.resolveUnitPrice(product, customer));
        BigDecimal lineTotal = money(unitPrice.multiply(qty));
        QuotationItem item = new QuotationItem();
        item.setLineNo(q.getItems().size() + 1);
        item.setProduct(product);
        item.setProductSku(product.getSku());
        item.setProductName(product.getName());
        item.setUnit(product.getUnit());
        item.setQuantity(qty);
        item.setUnitPrice(unitPrice);
        item.setLineDiscount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setLineTotal(lineTotal);
        q.addItem(item);
    }

    private void finalizeQuoteTotals(Quotation q) {
        BigDecimal subtotal = q.getItems().stream()
                .map(QuotationItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        q.setSubtotal(money(subtotal));
        q.setTotalAmount(money(q.getSubtotal().subtract(q.getDiscountAmount()).add(q.getTaxAmount())));
    }

    private SalesOrder newOrder(
            String number, Customer customer, Quotation quote, SalesOrderStatus status, String notes) {
        SalesOrder order = new SalesOrder();
        order.setSoNumber(number);
        order.setCustomer(customer);
        order.setQuotation(quote);
        order.setStatus(status);
        order.setNotes(notes);
        order.setDiscountAmount(quote.getDiscountAmount());
        order.setTaxAmount(quote.getTaxAmount());
        order.setCreatedBy("system");
        order.setUpdatedBy("system");
        return order;
    }

    private void copyQuoteToOrder(Quotation quote, SalesOrder order) {
        for (QuotationItem qi : quote.getItems()) {
            SalesOrderItem item = new SalesOrderItem();
            item.setLineNo(qi.getLineNo());
            item.setProduct(qi.getProduct());
            item.setProductSku(qi.getProductSku());
            item.setProductName(qi.getProductName());
            item.setUnit(qi.getUnit());
            item.setQuantityOrdered(qi.getQuantity());
            item.setQuantityDelivered(BigDecimal.ZERO);
            item.setQuantityInvoiced(BigDecimal.ZERO);
            item.setUnitPrice(qi.getUnitPrice());
            item.setLineDiscount(qi.getLineDiscount());
            item.setLineTotal(qi.getLineTotal());
            order.addItem(item);
        }
        order.setSubtotal(quote.getSubtotal());
        order.setTotalAmount(quote.getTotalAmount());
    }

    private FulfillmentInvoice newInvoice(
            String number,
            SalesOrder order,
            Customer customer,
            PaymentMethod method,
            FulfillmentInvoiceStatus status,
            String notes) {
        FulfillmentInvoice inv = new FulfillmentInvoice();
        inv.setInvoiceNumber(number);
        inv.setSalesOrder(order);
        inv.setCustomer(customer);
        inv.setPaymentMethod(method);
        inv.setStatus(status);
        inv.setNotes(notes);
        inv.setCreatedBy("system");
        inv.setUpdatedBy("system");
        return inv;
    }

    private void addInvoiceLine(FulfillmentInvoice inv, SalesOrderItem soItem, BigDecimal qty) {
        FulfillmentInvoiceItem item = new FulfillmentInvoiceItem();
        item.setSalesOrderItem(soItem);
        item.setLineNo(inv.getItems().size() + 1);
        item.setProduct(soItem.getProduct());
        item.setProductSku(soItem.getProductSku());
        item.setProductName(soItem.getProductName());
        item.setUnit(soItem.getUnit());
        item.setQuantity(qty);
        item.setUnitPrice(soItem.getUnitPrice());
        item.setLineDiscount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        item.setLineTotal(money(soItem.getUnitPrice().multiply(qty)));
        inv.addItem(item);
    }

    private Customer requireCustomer(String code) {
        return customerRepository
                .findByCustomerCodeIgnoreCaseAndDeletedAtIsNull(code)
                .orElseThrow(() -> new IllegalStateException("Demo customer missing: " + code));
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
