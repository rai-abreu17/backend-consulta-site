package br.com.terreiroreisebastiao.shared.persistence;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;

import java.util.EnumSet;

/**
 * Gera UUIDs no banco usando {@code gen_random_uuid()} no próprio INSERT.
 */
public class DatabaseGeneratedUuidGenerator implements OnExecutionGenerator {

    @Override
    public boolean referenceColumnsInSql(Dialect dialect) {
        return true;
    }

    @Override
    public boolean writePropertyValue() {
        return false;
    }

    @Override
    public String[] getReferencedColumnValues(Dialect dialect) {
        return new String[]{"gen_random_uuid()"};
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }
}