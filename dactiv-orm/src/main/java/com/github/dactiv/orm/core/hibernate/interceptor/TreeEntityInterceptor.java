package com.github.dactiv.orm.core.hibernate.interceptor;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.List;

import com.github.dactiv.common.utils.ConvertUtils;
import com.github.dactiv.common.utils.ReflectionUtils;
import com.github.dactiv.orm.annotation.TreeEntity;
import com.github.dactiv.orm.core.hibernate.support.BasicHibernateDao;
import com.github.dactiv.orm.interceptor.OrmDeleteInterceptor;
import com.github.dactiv.orm.interceptor.OrmInsertInterceptor;
import com.github.dactiv.orm.interceptor.OrmSaveInterceptor;
import com.github.dactiv.orm.interceptor.OrmUpdateInterceptor;

/**
 * 树形实体拦截器
 * 
 * @author maurice
 * 
 * @param <E>
 *            持久化对象类型
 * @param <ID>
 *            id主键类型
 */
public class TreeEntityInterceptor<E, ID extends Serializable> implements
		OrmSaveInterceptor<E, BasicHibernateDao<E, ID>>,
		OrmUpdateInterceptor<E, BasicHibernateDao<E, ID>>, 
		OrmInsertInterceptor<E, BasicHibernateDao<E, ID>>,
		OrmDeleteInterceptor<E, BasicHibernateDao<E, ID>>{

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmSaveInterceptor#onSave(java.lang.Object, java.lang.Object, java.lang.String, java.lang.String, java.io.Serializable)
	 */
	@Override
	public boolean onSave(E entity, BasicHibernateDao<E, ID> persistentContext,Serializable id) {

		Class<?> entityClass = ReflectionUtils.getTargetClass(entity);
		TreeEntity treeEntity = ReflectionUtils.getAnnotation(entityClass,TreeEntity.class);
		
		if (treeEntity != null) {
			E parent = ReflectionUtils.invokeGetterMethod(entity,treeEntity.parentProperty());
			// 如果对象父类不为null，将父类的leaf设置成true，表示父类下存在子节点
			if (parent != null) {
				Object value = ConvertUtils.convertToObject(treeEntity.hasLeafValue(), treeEntity.leafClass());
				ReflectionUtils.invokeSetterMethod(parent,treeEntity.leafProperty(), value);
			}
		}

		return Boolean.TRUE;
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmSaveInterceptor#onPostSave(java.lang.Object, java.lang.Object, java.lang.String, java.lang.String, java.io.Serializable)
	 */
	@Override
	public void onPostSave(E entity,BasicHibernateDao<E, ID> persistentContext,Serializable id) {
		
		Class<?> entityClass = ReflectionUtils.getTargetClass(entity);
		TreeEntity treeEntity = ReflectionUtils.getAnnotation(entityClass,TreeEntity.class);

		if (treeEntity != null) {
			//from {0} tree where tree.{1} = {2} and (select count(c) from {0} c where c.{3}.{4} = r.{4}) = {5}
			String hql = MessageFormat.format(treeEntity.refreshHql(),
					persistentContext.getEntityName(),
					treeEntity.leafProperty(),
					treeEntity.hasLeafValue(),
					treeEntity.parentProperty(), 
					persistentContext.getIdName(), 
					treeEntity.notHasLeafValue());
			
			List<E> list = persistentContext.findByQuery(hql);

			for (E e : list) {
				Object value = ConvertUtils.convertToObject(treeEntity.notHasLeafValue(), treeEntity.leafClass());
				ReflectionUtils.invokeSetterMethod(e,treeEntity.leafProperty(), value);
				persistentContext.merge(e);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmUpdateInterceptor#onUpdate(java.lang.Object, java.lang.Object, java.lang.String, java.lang.String, java.io.Serializable)
	 */
	@Override
	public boolean onUpdate(E entity,BasicHibernateDao<E, ID> persistentContext, Serializable id) {

		return onSave(entity, persistentContext,id);
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmUpdateInterceptor#onPostUpdate(java.lang.Object, java.lang.Object, java.lang.String, java.lang.String, java.io.Serializable)
	 */
	@Override
	public void onPostUpdate(E entity,BasicHibernateDao<E, ID> persistentContext, Serializable id) {

		onPostSave(entity, persistentContext, id);

	}
	
	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmInsertInterceptor#onInsert(java.lang.Object, java.lang.Object, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean onInsert(E entity,BasicHibernateDao<E, ID> persistentContext) {
		
		return onSave(entity, persistentContext,null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmInsertInterceptor#onPostInsert(java.lang.Object, java.lang.Object, java.lang.String, java.lang.String, java.io.Serializable)
	 */
	@Override
	public void onPostInsert(E entity,BasicHibernateDao<E, ID> persistentContext, Serializable id) {
		
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmDeleteInterceptor#onDelete(java.io.Serializable, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean onDelete(Serializable id, E entity, BasicHibernateDao<E, ID> persistentContext) {
		
		return Boolean.TRUE;
	}

	/*
	 * (non-Javadoc)
	 * @see com.github.dactiv.orm.interceptor.OrmDeleteInterceptor#onPostDelete(java.io.Serializable, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void onPostDelete(Serializable id, E entity, BasicHibernateDao<E, ID> persistentContext) {
		onPostSave(entity, persistentContext, id);
		
	}

}