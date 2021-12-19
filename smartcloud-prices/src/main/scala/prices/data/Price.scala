package prices.data

import java.time.Instant

final case class Price(kind: InstanceKind, price: BigDecimal, timestamp: Instant)
